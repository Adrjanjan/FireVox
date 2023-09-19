package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val conductionCalculator: ConductionCalculator,
    private val convectionCalculator: ConvectionCalculator,
    private val burningCalculator: BurningCalculator,
    private val ignitionCalculator: IgnitionCalculator,
    private val smokeCalculator: SmokeCalculator,
    @Value("\${firevox.timestep}")
    private val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
    private val simulationRepository: SimulationsRepository,
    private val materialRepository: PhysicalMaterialRepository,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    /**
     * Only voxel with the given voxelKey is updated
     * @param key
     */
    @Transactional
    fun calculate(key: VoxelKey, iteration: Int) {
//        verifySynchronization(iteration)
        val voxel = voxelRepository.findForIteration(key, iteration)
            ?: throw InvalidSimulationState("Calculation for voxel with key $key can't be made - voxel not found")

        if (voxel.isBoundaryCondition) {
//            log.info("Voxel is boundary condition")
            setNextProperties(voxel, iteration, listOf())
            return
        }

        val modelSize = simulationRepository.fetchSize()
        val (foundNeighbors, validKeysWithMissingVoxel) = voxelRepository.findNeighbors(
            voxel.key,
            NeighbourhoodType.N_E_W_S_U_L_,
            iteration,
            modelSize
        )

        val neighbours = fillMissingVoxelsInsideModel(foundNeighbors, validKeysWithMissingVoxel)
            .map { it.toVoxelState(iteration) }
        val voxelState = voxel.toVoxelState(iteration)

        // heat transfer calculators
        val conductionResult = if (voxelState.material.isSolid()) {
            conductionCalculator.calculate(voxelState, neighbours, timeStep)
        } else 0.0
        val convectionResult = if (voxelState.material.isFluid())
          convectionCalculator.calculate(voxelState, neighbours, timeStep)
        else 0.0
        val burningResult = if (voxelState.material.isFlammable())
          burningCalculator.calculate(voxelState, timeStep, iteration)
        else 0.0 to voxelState.material
        val heatResults = listOf(
            conductionResult,
            convectionResult,
            burningResult.first
        )

        // state change calculators
//        ignitionCalculator.calculate(voxelState, timeStep, iteration)
//        smokeCalculator.calculate(voxel, neighbours, timeStep)

        setNextProperties(voxel, iteration, heatResults)
        voxelRepository.save(voxel)
//        sendVoxelsForNextIteration(voxel) // każda z metod wyżej musi jeszcze zwracać voxel, który wymaga zmiany?
//        wtedy ta zmiana dopiero w następnej iteracji? czy już w tej?
        // załóżmy, że dolny wysyła energię do środkowego
        // wtedy środkowy musi pobrać energię w tej samej iteracji, żeby była zachowana spójność (energia nie znika)
        // górny nie otrzymuje żadnej energii od środkowego w tej iteracji, ponieważ środkowy tylko odbiera
        handleVirtualThermometer(key, voxel)
    }

    private fun fillMissingVoxelsInsideModel(
        neighbours: List<Voxel>,
        validKeysWithMissingVoxel: Set<VoxelKey>,
    ): List<Voxel> {
        val first = neighbours.first()
        val air: PhysicalMaterial = materialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        neighbours.toMutableList().addAll(validKeysWithMissingVoxel.map {
            Voxel(
                it,
                version = 0,
                evenIterationNumber = first.evenIterationNumber,
                evenIterationMaterial = air,
                evenIterationTemperature = air.baseTemperature,
                oddIterationNumber = first.oddIterationNumber,
                oddIterationMaterial = air,
                oddIterationTemperature = air.baseTemperature
            )
        })
        return neighbours;
    }

    private fun handleVirtualThermometer(key: VoxelKey, voxel: Voxel) {
        if (virtualThermometerService.check(key)) {
            virtualThermometerService.update(key, voxel.oddIterationTemperature)
        }
    }

    private fun setNextProperties(voxel: Voxel, iteration: Int, heatResults: List<Double>) {
        return when (iteration % 2) {
            0 -> {
                val resultTemp = voxel.evenIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.evenIterationMaterial
                voxel.oddIterationNumber = iteration
                voxel.oddIterationTemperature = resultTemp
                voxel.oddIterationMaterial = resultMaterial
            }

            1 -> {
                val resultTemp = voxel.oddIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.oddIterationMaterial
                voxel.evenIterationNumber = iteration
                voxel.evenIterationTemperature = resultTemp
                voxel.evenIterationMaterial = resultMaterial
            }

            else -> throw InvalidSimulationState("Number modulo 2 can't have value other than 0 or 1 ")
        }
    }

    private fun verifySynchronization(iteration: Int) {
        //send request to state verifier if counters for a given iteration are equal
        //     if yes start calculation
        //     if no, resend the key on the queue and log it
        TODO("Not yet implemented")
    }

    fun calculateRadiation(key: VoxelKey, iteration: Int) {
        TODO("Not yet implemented")
    }

}

data class VoxelState(
    val key: VoxelKey,
    var iterationNumber: Int,
    var material: PhysicalMaterial,
    var temperature: Double,
    var burningEndIteration: Int = -1,
)

class InvalidSimulationState(s: String) : Throwable(s)

fun Voxel.toVoxelState(iteration: Int) = when (iteration % 2) {
    0 -> VoxelState(this.key, this.evenIterationNumber, this.evenIterationMaterial, this.evenIterationTemperature,)
    1 -> VoxelState(this.key, this.oddIterationNumber, this.oddIterationMaterial, this.oddIterationTemperature,)
    else -> throw InvalidSimulationState("Number modulo 2 can't have value other than 0 or 1 ")
}