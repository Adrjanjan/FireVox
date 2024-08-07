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
        "firevox.voxel.size=1",
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

        val slabMaterial = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 1000.0,
            baseTemperature = 1000.toKelvin(),
            thermalConductivityCoefficient = 1.0,
            convectionHeatTransferCoefficient = 1.0,
            specificHeatCapacity = 0.001,
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

        // xy +0.15 | xyz *10
        // x 0, 30
        // y 0, 30
        // z 0, 30
        // hot slab = 10,20,10,20,0,10

        val voxels = (0 until 30).flatMap { x ->
            (0 until 30).flatMap { y ->
                (0 until 40).map { z ->
                    val (temp, material) = if (z < 10)
                        (if (x in 10 until 20 && y in 10 until 20)
                            1000.toKelvin()
                        else
                            0.toKelvin()) to slabMaterial
                    else
                        0.toKelvin() to air

                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = material,
                        evenIterationTemperature = temp,
                        oddIterationMaterial = material,
                        oddIterationTemperature = temp,
                        isBoundaryCondition = false
                    )
                }
            }
        }.toMutableList()

        val hotPlateThermometer = VoxelKey(15, 15, 0)
        val gasThermometer = VoxelKey(15, 15, 20)
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

                FileOutputStream("convection_ver/gasThermometer.csv").use {
                    val v = virtualThermometerService.getMeasurements(gasThermometer).also(
                        RadiationSimpleExecutionTest.log::info
                    ).replace(".", ",")
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }
            }
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}