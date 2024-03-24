package pl.edu.agh.firevox.worker.physics

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
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import java.io.FileOutputStream
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.1",
        "firevox.voxel.size=0.01",
        "firevox.smokeIntoFireThreshold=430",
    ],
    classes = [WorkerApplication::class, ItTestConfig::class]
)
class SmallCubeSmokeExecutionTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    val chunkRepository: ChunkRepository,
) : ShouldSpec({

    context("calculate smoke transfer test") {
        val simulationTimeInSeconds = 100 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        // given
        val smoke = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.SMOKE)
        val air = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        val concrete = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.CONCRETE)

        val voxels = mutableListOf<Voxel>()
//        val scale = 1
//        (0 until 10 * scale).forEach { x ->
//            (0 until 10 * scale).forEach { y ->
//                (0 until 10 * scale).forEach { z ->
//                    val (temp, material) = when {
//                        x in (3..5) && y in (3..5) && z in (3..5) -> 20.toKelvin() to concrete
//                        x in (3..5) && y in (3..5) && z in (1..2) -> 100.toKelvin() to smoke
//                        else -> 20.toKelvin() to air
//                    }
//                    voxels.add(
//                        Voxel(
//                            VoxelKey(x, y, z),
//                            evenIterationMaterial = material,
//                            evenIterationTemperature = temp,
//                            oddIterationMaterial = material,
//                            oddIterationTemperature = temp,
//                        )
//                    )
//                }
//            }
//        }
//        val scale = 1
        (0 .. 2).forEach { x ->
            (0 .. 2).forEach { y ->
                (0 .. 2).forEach { z ->
                    val (temp, material, smokeConcentration) = when {
                        x == 1 && y == 1 && z == 1  -> Triple(20.toKelvin(), concrete, 0.0)
                        x == 1 && y == 1 && z == 0  -> Triple(20.toKelvin(), smoke, 1.0)
                        else -> Triple(20.toKelvin(), air, 0.0)
                    }
                    voxels.add(
                        Voxel(
                            VoxelKey(x, y, z),
                            evenIterationMaterial = material,
                            evenIterationTemperature = temp,
                            evenSmokeConcentration = smokeConcentration,
                            oddIterationMaterial = material,
                            oddIterationTemperature = temp,
                            oddSmokeConcentration = smokeConcentration,
                        )
                    )
                }
            }
        }

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        Simulation(
            name = "Smoke Test",
            parentModel = SingleModel(name = "Parent model"),
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
        ).also(simulationsRepository::save)

        log.info("Model read from file")

        voxelRepository.saveAll(voxels)
        voxelRepository.flush()
        log.info("Model saved to DB ${voxelRepository.count()}")

        VoxFormatParser.write(
            voxels.filter { it.evenIterationMaterial.voxelMaterial != VoxelMaterial.AIR }
                .associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
            Palette.basePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("cube_mat_smoke_result_start.vox")
        )
        VoxFormatParser.write(
            voxels.associate {
                it.key to VoxFormatParser.toPaletteLinear(
                    value = it.evenIterationTemperature.toCelsius(),
                    min = 20.0,
                    max = 710.0
                )
            },
            Palette.temperaturePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("cube_temp_smoke_result_start.vox")
        )
        log.info("Start models saved to file")
        // when
        should("execute test") {
            val iterationNumber = 15 //(simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX-1, sizeY-1, sizeZ-1))
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                calculationService.calculateForChunk(chunk, i)
                log.info("Finished main calculator")

                if (true ){ //i % 100 == 0) {
                    val result = chunk.flatten()
                    val min = result.minOf { it.evenIterationTemperature }
                    val max = result.maxOf { it.evenIterationTemperature }
                    VoxFormatParser.write(
                        result.associate {
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
                        FileOutputStream("cube_temp_smoke_result_${i}s.vox")
                    )

                    VoxFormatParser.write(
                        result.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
                        Palette.basePalette,
                        sizeX,
                        sizeY,
                        sizeZ,
                        FileOutputStream("cube_mat_smoke_result_${i}s.vox")
                    )
                    log.info("Saved file for iteration $i")
                }
            }
            val result = chunk.flatten()
            val min = result.minOf { it.evenIterationTemperature }
            val max = result.maxOf { it.evenIterationTemperature }
            log.info("End of the processing, starting to write result, max temp: ${max.toCelsius()}, min temp: ${min.toCelsius()}")
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
