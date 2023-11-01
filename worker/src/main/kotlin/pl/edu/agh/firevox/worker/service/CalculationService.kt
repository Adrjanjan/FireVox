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
    private val smokeTransferCalculator: SmokeCalculator,
    @Value("\${firevox.timestep}") private val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
    private val simulationRepository: SimulationsRepository,
    private val materialRepository: PhysicalMaterialRepository,
    private val countersRepository: CountersRepository,
    private val voxelProcessingMessageSender: VoxelProcessingMessageSender,
    private val physicalMaterialRepository: PhysicalMaterialRepository,
    @Value("\${firevox.smokeIntoFireThreshold}") private val smokeIntoFireThreshold: Double = 150 + 273.15
) {

    private val burningToBurnt = mapOf(
        VoxelMaterial.TEXTILE_BURNING to VoxelMaterial.TEXTILE_BURNT,
        VoxelMaterial.WOOD_BURNING to VoxelMaterial.WOOD_BURNT,
        VoxelMaterial.PLASTIC_BURNING to VoxelMaterial.PLASTIC_BURNT,
    )

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Only voxel with the given voxelKey is updated
     * @return returns true if the calculation was performed, false otherwise
     */
    @Transactional
    fun calculate(key: VoxelKey, iteration: Int): Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) {
            return false
        }

        val voxel = voxelRepository.findByIdOrNull(key)
            ?: throw InvalidSimulationState("Calculation for voxel with key $key can't be made - voxel not found")

        if (voxel.lastProcessedIteration >= iteration) {
            // only '=' is valid but for the sake of not crashing
            return true
        }
        val voxelState = voxel.toVoxelState(iteration)

        handleVirtualThermometer(key, voxelState)

//        log.info("Processing voxel $voxel for iteration $iteration")

        if (voxel.isBoundaryCondition) {
            setNextProperties(voxel, iteration, listOf(), null, 0.0, voxel.ignitingCounter, voxel.burningCounter)
            return true
        }

        val voxelsToSendForSameIteration = mutableListOf<VoxelKey>()
        val modelSize = simulationRepository.fetchSize()
        val (foundNeighbors, validKeysWithMissingVoxel) = voxelRepository.findNeighbors(
            voxel.key, NeighbourhoodType.N_E_W_S_U_L_, modelSize
        )

        val neighbours =
            fillMissingVoxelsInsideModel(foundNeighbors, validKeysWithMissingVoxel).map { it.toVoxelState(iteration) }

        // heat transfer calculators
        val conductionResult = if (voxelState.material.isSolid() || voxelState.material.isFluid()) {
            conductionCalculator.calculate(voxelState, neighbours, timeStep, voxelsToSendForSameIteration)
        } else 0.0

        val convectionResult = if (voxelState.material.isFluid()) convectionCalculator.calculate(
            voxelState, neighbours, timeStep, voxelsToSendForSameIteration
        )
        else 0.0

        val burningResult = if (voxelState.material.isBurning()) burningCalculator.calculate(
            voxelState, timeStep, iteration, voxelsToSendForSameIteration
        ) else 0.0
        val heatResults = listOf(
            conductionResult, convectionResult, burningResult
        )

        val smokeUpdate = if (voxelState.material.transfersSmoke()) {
            smokeTransferCalculator.calculate(voxelState, neighbours, timeStep, iteration)
        } else 0.0

        // state change calculations
        val newMaterial = nextPhysicalMaterial(voxelState, neighbours, iteration, smokeUpdate)

        setNextProperties(voxel, iteration, heatResults, newMaterial, smokeUpdate, null, null)
        voxelRepository.save(voxel)

//        voxelsToSendForSameIteration.also {
//            countersRepository.add(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, it.size)
//        }.forEach {
//            voxelProcessingMessageSender.send(it, iteration)
//        }

        countersRepository.increment(CounterId.PROCESSED_VOXEL_COUNT)
        countersRepository.increment(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT)
        return true
    }

    private fun nextPhysicalMaterial(
        voxelState: VoxelState,
        neighbours: List<VoxelState>,
        iteration: Int,
        smokeUpdate: Double
    ) = when {
        // 1 air -> smoke
        voxelState.material.voxelMaterial == VoxelMaterial.AIR && smokeUpdate > 0
        -> physicalMaterialRepository.findByVoxelMaterial(
            if (voxelState.temperature < smokeIntoFireThreshold) VoxelMaterial.SMOKE else VoxelMaterial.FLAME
        )

        // 2 smoke -> flame && flame start
        voxelState.material.transfersSmoke() // is air/smoke but voxel below started burning
                && neighbours.firstOrNull { it.key.isBelow(voxelState.key) }?.material?.isBurning() == true
                || voxelState.material.voxelMaterial == VoxelMaterial.SMOKE && voxelState.temperature > smokeIntoFireThreshold
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.FLAME)

        // 3 flame -> air
        voxelState.material.voxelMaterial == VoxelMaterial.FLAME
                && voxelState.temperature < smokeIntoFireThreshold
                && voxelState.smokeConcentration == 0.0
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        // 4 air -> flame
        voxelState.material.voxelMaterial == VoxelMaterial.AIR
                && voxelState.temperature > smokeIntoFireThreshold
                && voxelState.smokeConcentration > 0.0
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.FLAME)

        // 5 flame -> smoke
        voxelState.material.voxelMaterial == VoxelMaterial.FLAME
                && voxelState.temperature < smokeIntoFireThreshold
                && voxelState.smokeConcentration > 0.0
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.SMOKE)

        // 6 smoke -> air
        voxelState.material.transfersSmoke() && voxelState.smokeConcentration + smokeUpdate == 0.0
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        // 7 glass -> air
        voxelState.material.voxelMaterial == VoxelMaterial.GLASS
                && voxelState.temperature >= voxelState.material.deformationTemperature!!
        -> physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)

        // 11 flammable -> burning
        voxelState.material.isFlammable() // ignite flammable
        -> ignitionCalculator.calculate(voxelState, neighbours, timeStep, iteration)

        // 13 burning -> burnt
        // 15 burnt -> air
        voxelState.material.isBurning()
                && voxelState.burningCounter * timeStep > voxelState.material.burningTime!!
        -> if (voxelState.material.burnsCompletely)
            physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        else voxelState.material


        // 8 glass -> glass
        // 9 metal -> metal
        // 12 burning -> burning
        // 10 flammable -> flammable
        // 14 burnt -> burnt
        else -> voxelState.material
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
                oddIterationTemperature = air.baseTemperature,
            )
        })
        return neighbours.also(voxelRepository::saveAll)
    }

    private fun handleVirtualThermometer(key: VoxelKey, voxel: VoxelState) {
        if (virtualThermometerService.check(key)) {
            virtualThermometerService.update(key, voxel.temperature)
        }
    }

    private fun setNextProperties(
        voxel: Voxel,
        iteration: Int,
        heatResults: List<Double>,
        newMaterial: PhysicalMaterial?,
        smokeUpdate: Double,
        ignitingCounter: Int? = null,
        burningCounter: Int? = null,
    ) {
        // state not used outside
        voxel.lastProcessedIteration = iteration
        ignitingCounter?.also { voxel.ignitingCounter = it }
        burningCounter?.also { voxel.burningCounter = it }

        // state used outside voxel
        if (iteration % 2 == 0) {
            val resultTemp = voxel.evenIterationTemperature + heatResults.sum()
            voxel.oddIterationTemperature = resultTemp
            voxel.oddIterationMaterial = newMaterial ?: voxel.oddIterationMaterial
            voxel.oddSmokeConcentration = smokeUpdate
        } else {
            val resultTemp = voxel.oddIterationTemperature + heatResults.sum()
            voxel.evenIterationTemperature = resultTemp
            voxel.evenIterationMaterial = newMaterial ?: voxel.evenIterationMaterial
            voxel.evenSmokeConcentration = smokeUpdate
        }
    }

}

data class VoxelState(
    val key: VoxelKey,
    var material: PhysicalMaterial,
    var temperature: Double,
    val wasProcessedThisIteration: Boolean,
    val smokeConcentration: Double,
    var ignitingCounter: Int = 0,
    var burningCounter: Int = 0,
)

class InvalidSimulationState(s: String) : Throwable(s)

fun Voxel.toVoxelState(iteration: Int) = if (iteration % 2 == 0)
    VoxelState(
        this.key,
        this.evenIterationMaterial,
        this.evenIterationTemperature,
        this.lastProcessedIteration >= iteration,
        this.evenSmokeConcentration,
        this.ignitingCounter,
        this.burningCounter,
    ) else
    VoxelState(
        this.key,
        this.oddIterationMaterial,
        this.oddIterationTemperature,
        this.lastProcessedIteration >= iteration,
        this.oddSmokeConcentration,
        this.ignitingCounter,
        this.burningCounter,
    )