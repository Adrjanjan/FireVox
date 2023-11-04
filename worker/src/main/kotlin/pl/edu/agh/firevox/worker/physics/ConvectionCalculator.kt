package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.worker.service.VoxelState
import kotlin.math.pow

@Service
class ConvectionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

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
        voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double, voxelsToSend: MutableList<VoxelKey>
    ): Double {
        val lower = voxels.firstOrNull { it.key.isBelow(voxel.key) && it.material.isFluid() }
        val upper = voxels.firstOrNull { it.key.isAbove(voxel.key) && it.material.isFluid() }
        val currentMaterial = voxel.material
        val alpha = 1 / (currentMaterial.density * voxelLength.pow(5) * currentMaterial.specificHeatCapacity)
        val currentAsTd = upper?.also { voxelsToSend.add(it.key) }
            ?.let { alpha * (voxel.material.convectionHeatTransferCoefficient * voxel.temperature - it.material.convectionHeatTransferCoefficient * it.temperature) * timeStep }
            ?: 0.0
        val currentAsTu = lower?.also { voxelsToSend.add(it.key) }
            ?.let { alpha * (voxel.material.convectionHeatTransferCoefficient * voxel.temperature - it.material.convectionHeatTransferCoefficient * it.temperature) * timeStep }
            ?: 0.0
        //
        return voxel.temperature + currentAsTd + currentAsTu
    }

}