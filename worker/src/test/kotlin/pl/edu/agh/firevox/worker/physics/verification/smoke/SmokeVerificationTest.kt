package pl.edu.agh.firevox.worker.physics.verification.smoke

import io.kotest.core.spec.style.ShouldSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.shared.model.simulation.counters.Counter
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import pl.edu.agh.firevox.worker.service.VirtualThermometerService
import java.io.FileOutputStream
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.1",
        "firevox.voxel.size=0.01",
        "firevox.plane.size=10",
        "firevox.smokeIntoFireThreshold=1393.15",
        "firevox.voxel.ambient: 273.15"
    ],
    classes = [WorkerApplication::class]
)
class SmokeVerificationTest(
    val postgreSQLContainer: PostgreSQLContainer<*>,
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
    val chunkRepository: ChunkRepository,
) : ShouldSpec({

    context("save voxels from file") {
        val simulationTimeInSeconds = 10
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        val air = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        val woodMaterial = PhysicalMaterial(
            // unit in FDS
            VoxelMaterial.WOOD_BURNING,
            density = 1000.0, // kg/m^3
            baseTemperature = 1000.toKelvin(),  // C
            thermalConductivityCoefficient = 1.0, // W/(m*K)
            convectionHeatTransferCoefficient = 1.0,  // W/(m^2*K)
            specificHeatCapacity = 1.0, // w FDS w kJ/(kg*K)
            ignitionTemperature = 1500.0,
            burningTime = 10.0,
            timeToIgnition = 10.0,
            autoignitionTemperature = 2000.0,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = 0.001, // %/(m^2/s)
            deformationTemperature = null,
            emissivity = 0.0,
        ).also(physicalMaterialRepository::save)

        val metalMaterial = PhysicalMaterial(
            // unit in FDS
            VoxelMaterial.METAL,
            density = 1000.0, // kg/m^3
            baseTemperature = 1000.toKelvin(),  // C
            thermalConductivityCoefficient = 1.0, // W/(m*K)
            convectionHeatTransferCoefficient = 1.0,  // W/(m^2*K)
            specificHeatCapacity = 1.0, // w FDS w kJ/(kg*K)
            ignitionTemperature = null,
            burningTime = 10.0,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = 0.0, // %/(m^2/s)
            deformationTemperature = null,
            emissivity = 0.0,
        ).also(physicalMaterialRepository::save)

        // full
        val voxels = (0 until 40).flatMap { x ->
            (0 until 40).flatMap { y ->
                (0 until 40).map { z ->
                    val (temp, material) =
                        if (z == 0) {
                            1000.toKelvin() to if (x in 10 until 30 && y in 10 until 30) woodMaterial else metalMaterial
                        } else {
                            20.toKelvin() to air
                        }

                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = material,
                        evenIterationTemperature = temp,
                        oddIterationMaterial = material,
                        oddIterationTemperature = temp,
                        isBoundaryCondition = false,
                        ambienceInsulated = true
                    )
                }
            }
        }.toMutableList()

//        val hotPlateThermometer = VoxelKey(15, 15, 0)
//        virtualThermometerService.create(hotPlateThermometer)

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Smoke Verification test",
                parentModel = SingleModel(name = "Parent model"),
                sizeX = sizeX,
                sizeY = sizeY,
                sizeZ = sizeZ,
            )
        )

        voxelRepository.saveAll(voxels)
        voxelRepository.flush()

        VoxFormatParser.write(
            voxels.associate {
                it.key to VoxFormatParser.toPaletteLinear(
                    value = it.evenIterationTemperature,
                    min = 20.toKelvin(),
                    max = 1000.toKelvin()
                )
            },
            Palette.temperaturePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("smoke_ver/smoke_verification_temp_start.vox")
        )

        VoxFormatParser.write(
            voxels.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
            Palette.basePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("smoke_ver/smoke_verification_mat_start.vox")
        )
//        dumpDatabase(postgreSQLContainer, "home/smoke_ver_dump_0.sql", log)
//        loadDatabaseFromDump(postgreSQLContainer, "home/smoke_ver_dump_0.sql", log)

        should("execute test") {
            var iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX - 1, sizeY - 1, sizeZ - 1))
                calculationService.calculateForChunk(chunk, i)
                chunkRepository.saveAll(chunk)

                if (i % 10 == 0) {
                    log.info("Dumping files for iteration: $i")
                    VoxFormatParser.write(
                        chunk.flatten().filter { it.evenIterationMaterial.voxelMaterial != VoxelMaterial.AIR }
                            .associate {
                                it.key to VoxFormatParser.toPaletteLinear(
                                    value = it.evenSmokeConcentration,
                                    min = 0.0,
                                    max = 1.0
                                )
                            },
                        Palette.smokePalette,
                        sizeX,
                        sizeY,
                        sizeZ,
                        FileOutputStream("smoke_ver/smoke_verification_temp_${i}.vox")
                    )
                }

                log.info("Iteration end: $i")
                if (i == iterationNumber) {
                    iterationNumber = readln().toInt()
                }

            }
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

//pg_dump -U firevox -h 127.0.0.1 -p 5432 -d firevox -f /home/smoke_dump.sql -c