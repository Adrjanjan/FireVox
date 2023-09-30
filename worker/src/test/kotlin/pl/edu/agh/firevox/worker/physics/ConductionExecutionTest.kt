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

    @Value("\${firevox.timestep}")
    val timeStep: Double,

    ) : ShouldSpec({

    context("save voxels from file") {
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
                            version = 0,
                            evenIterationNumber = -1,
                            evenIterationMaterial = baseMaterial,
                            evenIterationTemperature = 20.toKelvin(),
                            oddIterationNumber = 0,
                            oddIterationMaterial = baseMaterial,
                            oddIterationTemperature = 20.0
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
            val simulationTimeInSeconds = 100 // * 60
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
//                log.info("Iteration: $i")
                voxels.parallelStream().forEach { v -> calculationService.calculate(v.key, i) }
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

private fun Double.toKelvin(): Double = this + 273.15
private fun Int.toKelvin(): Double = this + 273.15
private fun Double.toCelsius() = this - 273.15

