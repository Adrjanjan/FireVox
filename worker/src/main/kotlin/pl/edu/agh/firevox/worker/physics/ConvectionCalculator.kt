package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.worker.service.VoxelState

@Service
class ConvectionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    fun calculate(voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double) : Double {
        TODO("Not yet implemented")
    }

}