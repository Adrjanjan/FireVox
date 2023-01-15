package pl.edu.agh.firevox.worker.physics

import pl.edu.agh.firevox.shared.model.Voxel
import pl.edu.agh.firevox.shared.model.VoxelKey

interface PhysicsCalculator {

    fun calculate(voxel: Voxel, voxels: Map<VoxelKey, Voxel?>)
}