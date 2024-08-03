package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.pow

@Service
class ConductionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    fun calculate(
        voxel: VoxelState,
        voxels: List<VoxelState>,
        timeStep: Double,
        voxelsToSend: MutableSet<VoxelKey>
    ): Double {
        // volume and distance(=voxelLength) sie skrocily do area
        val constants = timeStep / (voxel.material.density * voxelLength.pow(2) * voxel.material.specificHeatCapacity)

        val result = constants * voxels.sumOf { conductiveHeat(it, voxel) }
        return result
    }

    // dT * condCoeff_avh
    private fun conductiveHeat(
        it: VoxelState,
        voxel: VoxelState
    ) = (it.temperature - voxel.temperature) *
            (it.material.thermalConductivityCoefficient + voxel.material.thermalConductivityCoefficient) / 2

}