package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.Voxel
import pl.edu.agh.firevox.shared.model.VoxelKey

@Service
class ConductionCalculator(
    voxelRepository: CustomVoxelRepository,
) : PhysicsCalculator {

    override fun calculate(voxel: Voxel, voxels: Map<VoxelKey, Voxel?>) {
        TODO("Not yet implemented")
    }
}