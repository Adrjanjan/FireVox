package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val calculators: List<PhysicsCalculator>,
    @Value("firevox.timestep")
    private val timeStep: Double,
) {

    /**
     * Only voxel with the given voxelKey is updated
     * @param voxelKey
     */
    @Transactional
    fun calculate(voxelKey: VoxelKey, iteration: Int) {
        val voxel = voxelRepository.findForIteration(voxelKey, iteration)
            ?: throw InvalidSimulationState("Calculation for voxel with key $voxelKey can't be made - voxel not found")
        voxel.currentProperties = voxel.nextProperties
        //TODO
        // - add virtual thermometer here
        // - add synchronization on iteration here


        val neighbours = voxelRepository.findNeighbors(voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, iteration)
        val resultStates = calculators.map { it.calculate(voxel, neighbours, timeStep) }.toList()

        voxel.nextProperties = createStateProperties(iteration + 1, resultStates)
        voxelRepository.save(voxel)
    }

    private fun createStateProperties(i: Int, resultStates: List<StateProperties>): StateProperties {
        val temperature = resultStates.sumOf { it.temperature }
        val material = resultStates.map { it.material }.first() // FOR NOW
        return StateProperties(i, material, temperature)
    }

    fun calculateRadiation(key: VoxelKey, iteration: Int) {
        TODO("Not yet implemented")
    }

}

class InvalidSimulationState(s: String) : Throwable(s)
