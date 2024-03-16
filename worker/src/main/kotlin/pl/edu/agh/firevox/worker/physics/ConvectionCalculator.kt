package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.pow

@Service
class ConvectionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    private val delta = 10e-5
    private val lPow5 = voxelLength.pow(5)
    /**
     * Only natural convection is included
     *
     * newTemp = current.Temp + deltaTempDown + deltaTempUp
     * currentAsTd = alpha * (current.temperature - upper.temperature) * timeStep // only if upper is fluid
     * currentAsTu = alpha * (current.temperature - lower.temperature) * timeStep // only if lower is fluid
     * alpha_prim = current.convectionCoeff / (m * C)
     * m = material.density * V
     * C = material.heatCapacity
     **/
    fun calculate(
        voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double, voxelsToSend: MutableSet<VoxelKey>
    ): Double {
        val lower = voxels.firstOrNull { it.isBelow(voxel) && it.material.isFluid() }
        val upper = voxels.firstOrNull { it.isAbove(voxel) && it.material.isFluid() }
        val currentMaterial = voxel.material
        val alpha = 1 / (currentMaterial.density * lPow5 * currentMaterial.specificHeatCapacity) //  1/mass(density * length.pow(3)) * area(length.pow(2)) * shc
        val currentAsTd = upper
            ?.let { alpha * (voxel.material.convectionHeatTransferCoefficient + it.material.convectionHeatTransferCoefficient)/2 * (it.temperature - voxel.temperature) * timeStep }
            ?.also { if(it > delta) voxelsToSend.add(upper.key) }
            ?: 0.0
        val currentAsTu = lower
            ?.let { alpha * (voxel.material.convectionHeatTransferCoefficient + it.material.convectionHeatTransferCoefficient)/2 * (it.temperature - voxel.temperature) * timeStep }
            ?.also { if(it > delta) voxelsToSend.add(lower.key) }
            ?: 0.0

        return currentAsTd + currentAsTu
    }

}