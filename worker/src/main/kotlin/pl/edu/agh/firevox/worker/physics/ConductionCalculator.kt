package pl.edu.agh.firevox.worker.physics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelMaterial
import pl.edu.agh.firevox.shared.model.VoxelMaterial.AIR
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.pow

@Service
class ConductionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
    @Value("\${firevox.voxel.ambient}") val ambientTemperature: Double,
    @Value("\${firevox.ambientConduction:true}") val enableAmbient: Boolean
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun calculate(
        voxel: VoxelState,
        voxels: List<VoxelState>,
        timeStep: Double,
        voxelsToSend: MutableSet<VoxelKey>
    ): Double {
        // volume and distance(=voxelLength) sie skrocily do area
        val constants = timeStep / (voxel.material.density * voxelLength.pow(2) * voxel.material.specificHeatCapacity)

        val result = constants * voxels.filter {
            voxel.material.isSolid() && it.material.isSolid()
                    || voxel.material.isFluid() && it.material.isFluid() // should only work for stationary - also include velocity but cant do
        }.sumOf { conductiveHeat(it, voxel) }
        return result
    }

    // dT * condCoeff_avg
    private fun conductiveHeat(
        it: VoxelState,
        voxel: VoxelState
    ) = (it.temperature - voxel.temperature) *
            (it.material.thermalConductivityCoefficient + voxel.material.thermalConductivityCoefficient) / 2

}