package pl.edu.agh.firevox.worker.physics

import pl.edu.agh.firevox.shared.model.StateProperties
import pl.edu.agh.firevox.shared.model.Voxel

interface PhysicsCalculator {

    fun calculate(voxel: Voxel, voxels: List<Voxel>, timeStep: Double) : StateProperties
}