package pl.edu.agh.firevox.worker.physics.verification.convection

import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.shared.model.simulation.counters.Counter
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import java.io.FileOutputStream
import kotlin.math.roundToInt

import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.worker.physics.RadiationSimpleExecutionTest
import pl.edu.agh.firevox.worker.service.VirtualThermometerService
import java.io.OutputStreamWriter

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

        val slabMaterial = PhysicalMaterial( // unit in FDS
            VoxelMaterial.METAL,
            density = 1000.0, // kg/m^3
            baseTemperature = 1000.toKelvin(),  // C
            thermalConductivityCoefficient = 1000.0, // W/(m*K)
            convectionHeatTransferCoefficient = 1.0,  // W/(m^2*K)
            specificHeatCapacity = 1000.0, // kJ/(kg*K)
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
            emissivity = 0.0,
        ).also(physicalMaterialRepository::save)


        // &MESH IJK=3,3,3, XB=-0.15,0.15,-0.15,0.15,0.0,0.3 /
        // &VENT XB = -0.05,0.05,-0.05,0.05,0.0,0.0, SURF_ID='SLAB' /

//        A 1 m thick solid slab that is initially at 1000 °C is
//        suddenly exposed to air at 0 °C. The back of the slab is insulated. Its density is 1000 kg/m3,
//        its specific heat is 0.001 kJ/(kg·K), its conductivity is 1 W/(m·K),
//        and its emissivity is zero, meaning there is no radiative loss from the surface.
//        The convective heat transfer coefficient is 1 W/(m2·K).

        val voxels = (0 until 30).flatMap { x ->
            (0 until 30).flatMap { y ->
                (0 until 130).map { z ->
                    val (temp, material) = if (z < 100)
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

//        (10 until 20).flatMap { x ->
//            (10 until 20).flatMap { y ->
//                (0 until 100).map { z ->
//                    virtualThermometerService.create(VoxelKey(x, y, z))
//                }
//            }
//        }

        virtualThermometerService.create(hotPlateThermometer)
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

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                virtualThermometerService.updateDirectly(hotPlateThermometer, i)
                virtualThermometerService.updateDirectly(gasThermometer, i)
                virtualThermometerService.updateDirectly(surfaceThermometer, i)
//                (10 until 20).flatMap { x ->
//                    (10 until 20).flatMap { y ->
//                        (0 until 100).map { z ->
//                            virtualThermometerService.updateDirectly(VoxelKey(x, y, z), i)
//                        }
//                    }
//                }

                val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX - 1, sizeY - 1, sizeZ - 1))
                calculationService.calculateForChunk(chunk, i)
                chunkRepository.saveAll(chunk)

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
                log.info("Iteration end: $i")
            }

            withContext(Dispatchers.IO) {
                FileOutputStream("convection_ver/hotPlateThermometer.csv").use {
                    val v = virtualThermometerService.getMeasurements(hotPlateThermometer).also(
                        RadiationSimpleExecutionTest.log::info
                    ).replace(".", ",")
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }

                FileOutputStream("convection_ver/surfaceThermometer.csv").use {
                    val v = virtualThermometerService.getMeasurements(surfaceThermometer).also(
                        RadiationSimpleExecutionTest.log::info
                    ).replace(".", ",")
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }

                FileOutputStream("convection_ver/gasThermometer.csv").use {
                    val v = virtualThermometerService.getMeasurements(gasThermometer).also(
                        RadiationSimpleExecutionTest.log::info
                    ).replace(".", ",")
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
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