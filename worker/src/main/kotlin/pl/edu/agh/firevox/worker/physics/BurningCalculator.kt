package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.pow


@Service
class BurningCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {


    /**
     * Burning increases temperature inside voxel - chemical energy is changed to thermal
     *
     * deltaTemp = deltaQ/mC
     * m = material.density * V
     * C = material.heatCapacity
     * deltaQ = delta T * generatedEnergyPerSecond
    **/
    fun calculate(voxel: VoxelState, timeStep: Double, iteration: Int): Double {
        voxel.burningCounter += 1
        val volume: Double = voxelLength.pow(3)
        val currentMaterial = voxel.material
        val mass = currentMaterial.density * volume
        return (timeStep * currentMaterial.effectiveHeatOfCombustion!!) / (mass * currentMaterial.specificHeatCapacity)
    }


}