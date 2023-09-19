package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.PhysicalMaterial
import pl.edu.agh.firevox.worker.service.VoxelState

@Service
class SmokeCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    fun calculate(voxel: VoxelState, timeStep: Double, iteration: Int): PhysicalMaterial {
        TODO()
    }

}