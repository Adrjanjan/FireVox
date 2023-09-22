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
import pl.edu.agh.firevox.vox.VoxFormatParser
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import java.io.FileOutputStream
import kotlin.math.roundToInt

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
        (0..30 * scale).forEach { x ->
            (0..20 * scale).forEach { y ->
                (0..5 * scale).forEach { z ->
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
        voxels = voxels.filterNot { it.key.x in 11 * scale..19 * scale && it.key.y in 6 * scale..14 * scale }
            .toMutableList()

        // set boundary conditions
        voxels.filter { it.key.x == 0 }
            .forEach {
                it.evenIterationTemperature = 300.toKelvin()
                it.oddIterationTemperature = 300.toKelvin()
                it.isBoundaryCondition = true
            }
        voxels.filter { it.key.x == 20 * scale }
            .forEach { it.isBoundaryCondition = true }

        val sizeX = voxels.maxOf { it.key.x }
        val sizeY = voxels.maxOf { it.key.y }
        val sizeZ = voxels.maxOf { it.key.z }

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
            val iterationNumber = (100 / timeStep).roundToInt()
            for (i in 0..iterationNumber) {
                for (v in voxels) {
                    calculationService.calculate(v.key, i)
                }
            }

            val result = voxelRepository.findAll()
            result.forEach { log.info(it.toString()) }
            val min = result.minOf { it.evenIterationTemperature }
            val max = result.maxOf { it.evenIterationTemperature }

            VoxFormatParser.write(
                result.associate { it.key to VoxFormatParser.getBucketForValue(it.evenIterationTemperature, min, max, 256)},
                Palette.temperaturePalette,
                sizeX,
                sizeY,
                sizeZ,
                FileOutputStream("block_with_hole.vox")
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
