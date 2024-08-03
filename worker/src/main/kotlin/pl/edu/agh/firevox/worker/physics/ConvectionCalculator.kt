package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState

@Service
class ConvectionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
    @Value("\${firevox.voxel.ambient}") val ambientTemperature: Double,
) {

    /**
     * Only natural convection is included
     *
     * dT = timestep * dQ / (mass * specific_heat_capacity)
     * mass = density * voxelLength ^ 3
     * dQ = area * dT * h_avg
     **/
    fun calculate(
        voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double, voxelsToSend: MutableSet<VoxelKey>
    ): Double {
        val currentMaterial = voxel.material
        val constants = currentMaterial.density * voxelLength * currentMaterial.specificHeatCapacity

        var heatSum = voxels.sumOf { convectiveHeat(it, voxel, voxelsToSend) }

        if(voxels.size < 6) {
            heatSum += (6 - voxels.size) * heatLossToAmbience(voxel)
        }

        return timeStep * heatSum / constants
    }

    // <- dT * h_avg == dQ / area
    private fun convectiveHeat(
        other: VoxelState?,
        current: VoxelState,
        voxelsToSend: MutableSet<VoxelKey>
    ) = other
        ?.let { (it.temperature - current.temperature) * (current.material.convectionHeatTransferCoefficient + it.material.convectionHeatTransferCoefficient) / 2 }
        ?.also { voxelsToSend.add(other.key) }
        ?: 0.0

    private fun heatLossToAmbience(
        current: VoxelState
    ) = (ambientTemperature - current.temperature) * current.material.convectionHeatTransferCoefficient

}