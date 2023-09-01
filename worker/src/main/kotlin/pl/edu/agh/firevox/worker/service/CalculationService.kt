package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import jakarta.transaction.Transactional

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val calculator: PhysicsCalculator
) {

    /**
     * Only voxel with the given voxelKey is updated
     * @param voxelKey
     */
    @Transactional
    fun calculate(voxelKey: VoxelKey, iteration: Int) {
        val voxel = voxelRepository.findForIteration(voxelKey, iteration)
            ?: throw InvalidSimulationState("Calculation for voxel with key $voxelKey can't be made - voxel not found")
        calculator.calculate(voxel)
        voxelRepository.save(voxel)
    }

}

class InvalidSimulationState(s: String) : Throwable(s)
