package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.worker.service.VoxelState
import kotlin.math.pow

@Service
class ConductionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    fun calculate(voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double): Double {
        val volume: Double = voxelLength.pow(3)
        val currentMaterial = voxel.material

        val lambda = currentMaterial.thermalConductivityCoefficient /
                (currentMaterial.density * volume * currentMaterial.specificHeatCapacity)

        return lambda * voxelLength * timeStep * voxels.filter { includeInConduction(it, voxel) }
            .sumOf { (it.temperature - voxel.temperature) }
    }

    private fun includeInConduction(it: VoxelState, voxel: VoxelState) = it.key != voxel.key && it.material.isSolid()

}