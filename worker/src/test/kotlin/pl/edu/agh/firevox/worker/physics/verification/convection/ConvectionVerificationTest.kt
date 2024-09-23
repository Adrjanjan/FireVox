package pl.edu.agh.firevox.worker.physics.verification.convection

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
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.5",
        "firevox.voxel.size=0.01",
        "firevox.plane.size=10",
        "firevox.smokeIntoFireThreshold=593.15",
        "firevox.voxel.ambient: 273.15"
    ],
    classes = [WorkerApplication::class]
)
class ConvectionVerificationTest(
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
        val simulationTimeInSeconds = 1800
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        val air = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        val slabMaterial = PhysicalMaterial(
            // unit in FDS
            VoxelMaterial.METAL,
            density = 1000.0, // kg/m^3
            baseTemperature = 1000.toKelvin(),  // C
            thermalConductivityCoefficient = 1.0, // W/(m*K)
            convectionHeatTransferCoefficient = 1.0,  // W/(m^2*K)
            specificHeatCapacity = 1.0, // w FDS w kJ/(kg*K)
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
            emissivity = 0.0,
        ).also(physicalMaterialRepository::save)

        // full
        val voxels = (0 until 30).flatMap { x ->
            (0 until 30).flatMap { y ->
                (0 until 130).map { z ->
                    val (temp, material) = if (z < 100 && x in 10..19 && y in 10..19)
                        1000.toKelvin() to slabMaterial
                    else
                        0.toKelvin() to air

                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = material,
                        evenIterationTemperature = temp,
                        oddIterationMaterial = material,
                        oddIterationTemperature = temp,
                        isBoundaryCondition = false,
                        ambienceInsulated = z == 0 || z < 100 && (x in listOf(0, 29) && y in listOf(0, 29))
                    )
                }
            }
        }.toMutableList()

        val hotPlateThermometer = VoxelKey(15, 15, 0)
        val surfaceThermometer = VoxelKey(15, 15, 99)
        val gasThermometer = VoxelKey(15, 15, 120)

        virtualThermometerService.create(hotPlateThermometer)
        virtualThermometerService.create(surfaceThermometer)
        virtualThermometerService.create(gasThermometer)

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Convection Verification test",
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
                    max = 700.toKelvin()
                )
            },
            Palette.temperaturePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("convection_ver/convection_verification_temp_start.vox")
        )

        VoxFormatParser.write(
            voxels.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
            Palette.basePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("convection_ver/convection_verification_mat_start.vox")
        )
        dumpDatabase(postgreSQLContainer, "home/convection_ver_dump_0.sql", log)
//        loadDatabaseFromDump(postgreSQLContainer, "home/convection_ver_dump_0.sql", log)

        should("execute test") {
            var iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                if (i % 300 == 0) {
                    log.info("Dumping files for iteration: $i")
                    val min = voxels.minOf { it.evenIterationTemperature }
                    val max = voxels.maxOf { it.evenIterationTemperature }
                    VoxFormatParser.write(
                        voxels.associate {
                            it.key to VoxFormatParser.toPaletteLinear(
                                value = it.evenIterationTemperature,
                                min = min,
                                max = max
                            )
                        },
                        Palette.temperaturePalette,
                        sizeX,
                        sizeY,
                        sizeZ,
                        FileOutputStream("convection_ver/convection_verification_temp_${i}.vox")
                    )
                    log.info("Hot Plate dump $i")
                    dumpThermometer(
                        postgreSQLContainer,
                        hotPlateThermometer,
                        "hotPlateThermometer_$i.csv",
                        log
                    )

                    log.info("Surface dump $i")
                    dumpThermometer(
                        postgreSQLContainer,
                        surfaceThermometer,
                        "surfaceThermometer_$i.csv",
                        log
                    )

                    log.info("Gas dump $i")
                    dumpThermometer(
                        postgreSQLContainer,
                        gasThermometer,
                        "gasThermometer_$i.csv",
                        log
                    )
                }

                log.info("Iteration: $i")
                virtualThermometerService.updateDirectly(hotPlateThermometer, i)
                virtualThermometerService.updateDirectly(gasThermometer, i)
                virtualThermometerService.updateDirectly(surfaceThermometer, i)

                val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX - 1, sizeY - 1, sizeZ - 1))
                calculationService.calculateForChunk(chunk, i)
                chunkRepository.saveAll(chunk)

                log.info("Iteration end: $i")
                if (i == iterationNumber) {
                    iterationNumber = readln().toInt()
                }

                if (i == 100) {
                    dumpDatabase(postgreSQLContainer, "home/convection_ver_dump_$i.sql", log)
                }
                if (i % 500 == 0) {
                    dumpDatabase(postgreSQLContainer, "home/convection_ver_dump_$i.sql", log)
                }
            }
            assert(1 > 0)
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

private fun dumpThermometer(
    container: PostgreSQLContainer<*>,
    key: VoxelKey,
    path: String,
    logger: Logger
) {
    CompletableFuture.runAsync {
        logger.info("Dumping thermometer $path")
        val x = key.x
        val y = key.y
        val z = key.z

        val command = arrayOf(
            """psql -U ${container.username} -h ${container.host} -p 5432 -c "\copy (select v.measurement from virtual_thermometer v where v.x = $x and v.y = $y and v.z = $z) TO '/home/convection_ver/$path' WITH CSV HEADER" """
        )
        logger.info(command.joinToString(separator = " "))
        val execResult = container.execInContainer(*command)
        logger.info(execResult.toString())
    }
}

fun dumpDatabase(container: PostgreSQLContainer<*>, outputFilePath: String, logger: Logger) {
    CompletableFuture.runAsync {
        val command = arrayOf(
            "pg_dump",
            "-U", container.username,
            "-h", container.host,
            "-p", "5432",
            "-d", container.databaseName,
            "-f", outputFilePath,
            "-c",
        )
        logger.info(command.joinToString(separator = " "))
        val execResult = container.execInContainer(*command)
        logger.info(execResult.toString())
    }
}

fun loadDatabaseFromDump(container: PostgreSQLContainer<*>, inputFilePath: String, logger: Logger) {
    CompletableFuture.runAsync {
        val command = arrayOf(
            "psql",
            "-U", container.username,
            "-h", container.host,
            "-p", "5432",
            "-d", container.databaseName,
            "-f", inputFilePath,
        )
        logger.info(command.joinToString(separator = " "))
        val execResult = container.execInContainer(*command)
        logger.info(execResult.toString())
    }

}
