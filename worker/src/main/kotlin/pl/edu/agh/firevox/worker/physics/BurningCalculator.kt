package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.PhysicalMaterial
import pl.edu.agh.firevox.worker.service.VoxelState
import kotlin.math.pow


@Service
class BurningCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {


    /**
     * Burning increases temperature inside voxel - chemical energy is changed to thermal
     *
     * newTemp = current.Temp + deltaTemp
     * deltaTemp = deltaQ/mC
     * m = material.density * V
     * C = material.heatCapacity
     * deltaQ = delta T * generatedEnergyPerSecond
    **/
    fun calculate(voxel: VoxelState, timeStep: Double, iteration: Int): Pair<Double, PhysicalMaterial?> {
        val volume: Double = voxelLength.pow(3)
        val currentMaterial = voxel.material
        val mass = currentMaterial.density * volume

        val newTemp = (timeStep * currentMaterial.generatedEnergyDuringBurning) / (mass * currentMaterial.specificHeatCapacity)
        return newTemp to calculateNewMaterial(voxel, iteration)
    }

    private fun calculateNewMaterial(voxel: VoxelState, iteration: Int): PhysicalMaterial? {
        return if (voxel.burningEndIteration >= iteration) {
            voxel.material //.burntMaterial
        } else voxel.material
    }

}