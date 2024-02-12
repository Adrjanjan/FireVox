package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
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
import pl.edu.agh.firevox.worker.service.VirtualThermometerService

@SpringBootTest(
    properties = ["firevox.timestep=0.1", "firevox.voxel.size=0.01", "firevox.smokeIntoFireThreshold: 393.15"],
    classes = [WorkerApplication::class]
)
class SimpleSmokeVerificationTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
) : ShouldSpec({

    context("save voxels from file") {
        val simulationTimeInSeconds = 10 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        val wood = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.WOOD)
        val concrete = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.CONCRETE)
        val air = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        val voxels = (0..9).flatMap { x ->
            (0..9).map { y ->
                Voxel(
                    VoxelKey(x, y, 0),
                    evenIterationMaterial = concrete,
                    evenIterationTemperature = 20.toKelvin(),
                    oddIterationMaterial = concrete,
                    oddIterationTemperature = 20.toKelvin()
                )
            }
        }.toMutableList()

        voxels.addAll(
            (0..9).flatMap { x ->
                (0..9).map { y ->
                    Voxel(
                        VoxelKey(x, y, 1),
                        evenIterationMaterial = wood,
                        evenIterationTemperature = 700.toKelvin(),
                        oddIterationMaterial = wood,
                        oddIterationTemperature = 700.toKelvin()
                    )
                }
            }
        )

        voxels.addAll(
            (0..9).flatMap { x ->
                (0..9).flatMap { y ->
                    (2..9).map { z ->
                        Voxel(
                            VoxelKey(x, y, z),
                            evenIterationMaterial = air,
                            evenIterationTemperature = 20.toKelvin(),
                            oddIterationMaterial = air,
                            oddIterationTemperature = 20.toKelvin()
                        )
                    }
                }
            }
        )

        virtualThermometerService.create(voxels[0].key)
        virtualThermometerService.create(voxels[1].key)

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Conduction test",
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
            FileOutputStream("temp_smoke_start.vox")
        )

        VoxFormatParser.write(
            voxels.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
            Palette.basePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("mat_smoke_start.vox")
        )

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..1) {
                voxels//.parallelStream()
                    .forEach { v -> calculationService.calculateGivenVoxel(v, i) }
                countersRepository.increment(CounterId.CURRENT_ITERATION)

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
                    FileOutputStream("temp_smoke_result_${i}.vox")
                )

                VoxFormatParser.write(
                    voxels.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
                    Palette.basePalette,
                    sizeX,
                    sizeY,
                    sizeZ,
                    FileOutputStream("mat_smoke_result_${i}.vox")
                )
                log.info("Iteration end: $i")
            }
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}