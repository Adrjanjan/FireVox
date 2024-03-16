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
        "firevox.smokeIntoFireThreshold=150",
    ],
    classes = [WorkerApplication::class, ItTestConfig::class]
)
class SmokeExecutionTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    val chunkRepository: ChunkRepository,
) : ShouldSpec({

    context("calculate radiation test") {
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
        val input = withContext(Dispatchers.IO) {
            getFile("radiation_test.vox")
        }
        // when
        val parsed = VoxFormatParser.read(input)

        val wood = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.WOOD)
        val concrete = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.CONCRETE)
        val air = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        val voxels = parsed.voxels.map { (k, _) ->
            Voxel(
                VoxelKey(k.x, k.y, k.z),
                evenIterationMaterial = if (isWood(k)) wood else concrete,
                evenIterationTemperature = if (isWood(k)) 700.toKelvin() else 25.toKelvin(),
                oddIterationMaterial = if (isWood(k)) wood else concrete,
                oddIterationTemperature = if (isWood(k)) 700.toKelvin() else 25.toKelvin(),
                isBoundaryCondition = false //isBoundary(k)
            )
        }.toMutableList()

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        val model = Simulation(
            name = "Smoke Test",
            parentModel = SingleModel(name = "Parent model"),
            sizeX = sizeX,
            sizeY = sizeY,
            sizeZ = sizeZ,
        ).also(simulationsRepository::save)

        log.info("Model read from file")

        val matrix = Array(sizeX) { x ->
            Array(sizeY) { y ->
                IntArray(sizeZ) { z -> 0 }
            }
        }

        voxels.forEach {
            matrix[it.key.x][it.key.y][it.key.z] = it.evenIterationMaterial.voxelMaterial.colorId
        }

        matrix.forEachIndexed { x, array ->
            array.forEachIndexed { y, ints ->
                ints.forEachIndexed { z, i ->
                    if(i == 0) {
                        Voxel(
                            VoxelKey(x, y, z),
                            evenIterationMaterial = air,
                            evenIterationTemperature = 25.toKelvin(),
                            oddIterationMaterial = air,
                            oddIterationTemperature = 25.toKelvin(),
                            isBoundaryCondition = false
                        ).also(voxels::add)
                    }
                }
            }
        }

        voxelRepository.saveAll(voxels)
        voxelRepository.flush()
        log.info("Model saved to DB")

        VoxFormatParser.write(
            voxels.filter { it.evenIterationMaterial.voxelMaterial != VoxelMaterial.AIR }.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
            Palette.basePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("big_mat_smoke_result_start.vox")
        )
        VoxFormatParser.write(
            voxels.associate {
                it.key to VoxFormatParser.toPaletteLinear(
                    value = it.evenIterationTemperature.toCelsius(),
                    min = 25.0,
                    max = 700.0
                )
            },
            Palette.temperaturePalette,
            sizeX,
            sizeY,
            sizeZ,
            FileOutputStream("big_temp_smoke_result_start.vox")
        )
        log.info("Start models saved to file")

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX, sizeY, sizeZ))
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                calculationService.calculateForChunk(chunk, i)
                log.info("Finished main calculator")

                if (i % 100 == 0) {
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
                        FileOutputStream("temp_smoke_result_${i * timeStep}s.vox")
                    )
                    VoxFormatParser.write(
                        result.associate { it.key to it.oddIterationMaterial.voxelMaterial.colorId },
                        Palette.basePalette,
                        sizeX,
                        sizeY,
                        sizeZ,
                        FileOutputStream("mat_smoke_result_${i * timeStep}s.vox")
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

private fun isWood(k: VoxelKey) = k.x in 52..98 && k.z == 0 && k.y in 25..75