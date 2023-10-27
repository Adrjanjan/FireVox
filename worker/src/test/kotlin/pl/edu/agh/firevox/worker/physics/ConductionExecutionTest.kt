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
import kotlin.math.pow
import kotlin.math.sqrt

@SpringBootTest(
    properties = ["firevox.timestep=0.1", "firevox.voxel.size=0.01"],
    classes = [WorkerApplication::class]
)
class ConductionExecutionTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
) : ShouldSpec({

    context("save voxels from file") {
        val simulationTimeInSeconds = 100 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))


        var voxels = mutableListOf<Voxel>()
        val baseMaterial = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 2700.0,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 235.0,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 897.0,
            flashPointTemperature = 0.0.toKelvin(),
            burningTime = 0.0,
            generatedEnergyDuringBurning = 0.0,
            burntMaterial = null,
        ).also(physicalMaterialRepository::save)

        PhysicalMaterial(
            VoxelMaterial.AIR,
            density = 1.204,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 25.87,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 1.0061,
            flashPointTemperature = 0.0.toKelvin(),
            burningTime = 0.0,
            generatedEnergyDuringBurning = 0.0,
            burntMaterial = null
        ).also(physicalMaterialRepository::save)

        // scale - number of voxels per centimeter
        val scale = 1
        (0 until 30 * scale).forEach { x ->
            (0 until 20 * scale).forEach { y ->
                (0 until 5 * scale).forEach { z ->
                    voxels.add(
                        Voxel(
                            VoxelKey(x, y, z),
                            evenIterationMaterial = baseMaterial,
                            evenIterationTemperature = 20.toKelvin(),
                            oddIterationMaterial = baseMaterial,
                            oddIterationTemperature = 20.0.toKelvin()
                        )
                    )
                }
            }
        }
        // hole
        val xCenter = 15.0 * scale - 1
        val yCenter = 10.0 * scale - 1
        val holeRadius = 5.0 * scale
        voxels = voxels.filterNot {
            sqrt((it.key.x - xCenter).pow(2) + (it.key.y - yCenter).pow(2)) < holeRadius
        }.toMutableList()

        // set boundary conditions
        voxels.filter { it.key.x == 0 }
            .forEach {
                it.evenIterationTemperature = 300.toKelvin()
                it.oddIterationTemperature = 300.toKelvin()
                it.isBoundaryCondition = true
            }
        voxels.filter { it.key.x == 30 * scale - 1 }
            .forEach { it.isBoundaryCondition = true }

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

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
//                log.info("Iteration: $i")
                voxels.parallelStream().forEach { v -> calculationService.calculate(v.key, i) }
                countersRepository.increment(CounterId.CURRENT_ITERATION)
            }

            val result = voxelRepository.findAll()
//            result.forEach { log.info(it.toString()) }
            val min = result.minOf { it.evenIterationTemperature }
            val max = result.maxOf { it.evenIterationTemperature }
            log.info("End of the processing, starting to write result, max temp: ${max.toCelsius()}, min temp: ${min.toCelsius()}")

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
                FileOutputStream("block_with_hole_scale.vox")
            )
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}


