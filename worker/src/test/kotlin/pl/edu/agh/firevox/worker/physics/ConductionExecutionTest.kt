package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import kotlin.math.roundToInt

@SpringBootTest(
    properties = ["firevox.timestep=0.1", "firevox.voxel.size=0.01"],
    classes = [WorkerApplication::class]
)
class ConductionExecutionTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,

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
            generatedEnergyDuringBurning = 0.0
        ).also(physicalMaterialRepository::save)

        // box scale cm^3 voxels
        val scale = 1
        (0..20 * scale).forEach { x ->
            (0..10 * scale).forEach { y ->
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
        // set one wall to heated
        voxels.filter { it.key.x == 0 }
            .forEach { it.evenIterationTemperature = 300.toKelvin() }

        voxelRepository.saveAll(voxels)
        voxelRepository.flush()

        val iterationNumber = (10 / timeStep).roundToInt()
        should("execute test") {
            for (i in 0..iterationNumber) {
                for (v in voxels) {
                    calculationService.calculate(v.key, i)
                }
            }

            val result = voxelRepository.findAll()
            result.size shouldBeGreaterThan 0
        }
    }

})

private fun Double.toKelvin(): Double = this + 273.15
private fun Int.toKelvin(): Double = this + 273.15