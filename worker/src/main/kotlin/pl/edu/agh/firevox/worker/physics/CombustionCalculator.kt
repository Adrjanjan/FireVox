package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.PhysicalMaterial
import pl.edu.agh.firevox.shared.model.Voxel

@Service
class CombustionCalculator {
    fun calculate(voxel: Voxel, voxels: List<Voxel>) : Pair<Double, PhysicalMaterial> {
        TODO("Not yet implemented")
    }

}