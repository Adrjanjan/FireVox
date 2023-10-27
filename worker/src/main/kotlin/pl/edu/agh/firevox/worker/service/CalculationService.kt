package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import pl.edu.agh.firevox.shared.messaging.VoxelProcessingMessageSender
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val conductionCalculator: ConductionCalculator,
    private val convectionCalculator: ConvectionCalculator,
    private val burningCalculator: BurningCalculator,
    private val ignitionCalculator: IgnitionCalculator,
    private val smokeCalculator: SmokeCalculator,
    @Value("\${firevox.timestep}") private val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
    private val simulationRepository: SimulationsRepository,
    private val materialRepository: PhysicalMaterialRepository,
    private val countersRepository: CountersRepository,
    private val voxelProcessingMessageSender: VoxelProcessingMessageSender,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Only voxel with the given voxelKey is updated
     * @return returns true if the calculation was performed, false otherwise
     */
    @Transactional
    fun calculate(key: VoxelKey, iteration: Int) : Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) {
            return false
        }

        val voxel = voxelRepository.findByIdOrNull(key)
            ?: throw InvalidSimulationState("Calculation for voxel with key $key can't be made - voxel not found")

        if(voxel.lastProcessedIteration >= iteration) {
            // only '=' is valid but
            return true
        }
        handleVirtualThermometer(key, voxel) // before calculations to include radiation from previous iteration in saving

        if (voxel.isBoundaryCondition) {
            setNextProperties(voxel, iteration, listOf())
            return true
        }

        val voxelsToSendForSameIteration = mutableListOf<VoxelKey>()
        val modelSize = simulationRepository.fetchSize()
        val (foundNeighbors, validKeysWithMissingVoxel) = voxelRepository.findNeighbors(
            voxel.key,
            NeighbourhoodType.N_E_W_S_U_L_,
            modelSize
        )

        val neighbours = fillMissingVoxelsInsideModel(foundNeighbors, validKeysWithMissingVoxel)
            .map { it.toVoxelState(iteration) }
        val voxelState = voxel.toVoxelState(iteration)

        // heat transfer calculators
        val conductionResult = if (voxelState.material.isSolid()) {
            conductionCalculator.calculate(voxelState, neighbours, timeStep, voxelsToSendForSameIteration)
        } else 0.0
        val convectionResult = if (voxelState.material.isFluid())
          convectionCalculator.calculate(voxelState, neighbours, timeStep, voxelsToSendForSameIteration)
        else 0.0
        val burningResult = if (voxelState.material.isFlammable())
          burningCalculator.calculate(voxelState, timeStep, iteration, voxelsToSendForSameIteration)
        else 0.0 to voxelState.material
        val heatResults = listOf(
            conductionResult,
            convectionResult,
//            burningResult.first
        )

        // state change calculators
//        ignitionCalculator.calculate(voxelState, timeStep, iteration)
//        smokeCalculator.calculate(voxel, neighbours, timeStep)

        setNextProperties(voxel, iteration, heatResults)
        voxelRepository.save(voxel)

        voxelsToSendForSameIteration.also {
            countersRepository.add(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, it.size)
        }.forEach {
            voxelProcessingMessageSender.send(it, iteration)
        }

        countersRepository.increment(CounterId.PROCESSED_VOXEL_COUNT)
        countersRepository.increment(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT)
        return true
    }

    private fun fillMissingVoxelsInsideModel(
        neighbours: List<Voxel>,
        validKeysWithMissingVoxel: Set<VoxelKey>,
    ): List<Voxel> {
        val air: PhysicalMaterial = materialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        neighbours.toMutableList().addAll(validKeysWithMissingVoxel.map {
            Voxel(
                it,
                evenIterationMaterial = air,
                evenIterationTemperature = air.baseTemperature,
                oddIterationMaterial = air,
                oddIterationTemperature = air.baseTemperature
            )
        })
        return neighbours.also(voxelRepository::saveAll)
    }

    private fun handleVirtualThermometer(key: VoxelKey, voxel: Voxel) {
        if (virtualThermometerService.check(key)) {
            virtualThermometerService.update(key, voxel.oddIterationTemperature)
        }
    }

    private fun setNextProperties(voxel: Voxel, iteration: Int, heatResults: List<Double>) {
        voxel.lastProcessedIteration = iteration
        return when (iteration % 2) {
            0 -> {
                val resultTemp = voxel.evenIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.evenIterationMaterial
                voxel.oddIterationTemperature = resultTemp
                voxel.oddIterationMaterial = resultMaterial
            }

            1 -> {
                val resultTemp = voxel.oddIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.oddIterationMaterial
                voxel.evenIterationTemperature = resultTemp
                voxel.evenIterationMaterial = resultMaterial
            }

            else -> throw InvalidSimulationState("Number modulo 2 can't have value other than 0 or 1 ")
        }
    }

}

data class VoxelState(
    val key: VoxelKey,
    var material: PhysicalMaterial,
    var temperature: Double,
    val wasProcessedThisIteration: Boolean,
    var burningEndIteration: Int = -1,
)

class InvalidSimulationState(s: String) : Throwable(s)

fun Voxel.toVoxelState(iteration: Int) = when (iteration % 2) {
    0 -> VoxelState(this.key, this.evenIterationMaterial, this.evenIterationTemperature, this.lastProcessedIteration >= iteration)
    1 -> VoxelState(this.key, this.oddIterationMaterial, this.oddIterationTemperature, this.lastProcessedIteration >= iteration)
    else -> throw InvalidSimulationState("Number modulo 2 can't have value other than 0 or 1 ")
}