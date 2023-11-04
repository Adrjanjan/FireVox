package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.worker.service.VoxelState
import kotlin.math.abs
import kotlin.math.pow

@Service
class ConductionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    private val delta = 10e-5

    fun calculate(
        voxel: VoxelState,
        voxels: List<VoxelState>,
        timeStep: Double,
        voxelsToSend: MutableList<VoxelKey>
    ): Double {
        val volume: Double = voxelLength.pow(3)
        val currentMaterial = voxel.material

        val lambda = 1 /
                (currentMaterial.density * volume * currentMaterial.specificHeatCapacity)

        return lambda * voxelLength * timeStep * voxels.filter { includeInConduction(it, voxel) } .also { vs ->
            voxelsToSend.addAll(vs.filterNot { it.wasProcessedThisIteration }.map { it.key })
        }.sumOf { (it.material.thermalConductivityCoefficient * it.temperature - voxel.material.thermalConductivityCoefficient * voxel.temperature) }
    }

    private fun includeInConduction(it: VoxelState, voxel: VoxelState) = it.key != voxel.key
            && it.material.isSolid()
            && abs(it.temperature - voxel.temperature) > delta

}