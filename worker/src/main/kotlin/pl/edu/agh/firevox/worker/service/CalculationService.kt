package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import javax.transaction.Transactional

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val calculators: List<PhysicsCalculator>
) {

    /**
     * Only voxel with the given voxelKey is updated
     * @param voxelKey
     */
    @Transactional
    fun calculate(voxelKey: VoxelKey, iteration: Int) {
        val voxels = voxelRepository.findWithNeighbors(voxelKey, NeighbourhoodType.ALL, iteration)
        val mainVoxel = voxels[voxelKey]
            ?: throw InvalidSimulationState("Calculation for voxel with key $voxelKey can't be made - voxel not found")
        calculators.forEach {
            it.calculate(mainVoxel, voxels)
        }
        voxelRepository.save(mainVoxel)
    }

}

class InvalidSimulationState(s: String) : Throwable(s)
