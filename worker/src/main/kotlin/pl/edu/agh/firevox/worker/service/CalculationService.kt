package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.*
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository

@Service
class CalculationService(
    private val voxelRepository: CustomVoxelRepository,
    private val conductionCalculator: ConductionCalculator,
    @Value("\${firevox.timestep}")
    private val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
    private val simulationRepository: SimulationsRepository,
    private val materialRepository: PhysicalMaterialRepository,
) {

    /**
     * Only voxel with the given voxelKey is updated
     * @param key
     */
    @Transactional
    fun calculate(key: VoxelKey, iteration: Int) {
//        verifySynchronization(iteration)
        val voxel = voxelRepository.findForIteration(key, iteration)
            ?: throw InvalidSimulationState("Calculation for voxel with key $key can't be made - voxel not found")
        val modelSize = simulationRepository.fetchSize()
        val (foundNeighbors, validKeysWithMissingVoxel) = voxelRepository.findNeighbors(voxel.key, NeighbourhoodType.N_E_W_S_U_L_, iteration, modelSize)

        val neighbours =  fillMissingVoxelsInsideModel(foundNeighbors, validKeysWithMissingVoxel)
            .map { it.toVoxelState(iteration) }

        // heat transfer calculators
        val conductionResult = if (voxel.evenIterationMaterial.isSolid())
            conductionCalculator.calculate(voxel.toVoxelState(iteration), neighbours, timeStep)
        else 0.0
//        val convectionResult = if (voxel.currentMaterial.isFluid())
//          convectionCalculator.calculate(voxel, neighbours, timeStep)
//        else 0.0
//        val burningResult = if (voxel.currentMaterial.isFlammable())
//          burningCalculator.calculate(voxel, neighbours, timeStep)
//        else 0.0
        val heatResults = listOf(
            conductionResult,
//            convectionResult,
//            burningResult
        )

        // state change calculators
//        selfIgnitionCalculator.calculate(voxel, neighbours, timeStep)
//        combustionCalculator.calculate(voxel, neighbours, timeStep)

        setNextProperties(voxel, iteration, heatResults)
        voxelRepository.save(voxel)
//        sendVoxelsForNextIteration(voxel) // każda z metod wyżej musi jeszcze zwracać voxel, który wymaga zmiany?
//        wtedy ta zmiana dopiero w następnej iteracji? czy już w tej?
        // załóżmy, że dolny wysyła energię do środkowego
        // wtedy środkowy musi pobrać energię w tej samej iteracji, żeby była zachowana spójność (energia nie znika)
        // górny nie otrzymuje żadnej energii od środkowego w tej iteracji, ponieważ środkowy tylko odbiera
        handleVirtualThermometer(key, voxel)
    }

    private fun fillMissingVoxelsInsideModel(neighbours: List<Voxel>, validKeysWithMissingVoxel: Set<VoxelKey>, ) : List<Voxel> {
        val first = neighbours.first()
        val air : PhysicalMaterial = materialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
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
        val i = iteration + 1
        return when(iteration % 2) {
            0 -> {
                val resultTemp = voxel.evenIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.evenIterationMaterial
                voxel.oddIterationNumber = i
                voxel.oddIterationTemperature = resultTemp
                voxel.oddIterationMaterial = resultMaterial
            }
            1 -> {
                val resultTemp = voxel.oddIterationTemperature + heatResults.sum()
                val resultMaterial = voxel.oddIterationMaterial
                voxel.evenIterationNumber = i
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

data class VoxelState (
    val key: VoxelKey,
    var iterationNumber: Int,
    var material: PhysicalMaterial,
    var temperature: Double,
)

class InvalidSimulationState(s: String) : Throwable(s)

fun Voxel.toVoxelState(iteration: Int) = when(iteration % 2) {
    0 -> VoxelState(this.key, this.evenIterationNumber, this.evenIterationMaterial, this.evenIterationTemperature)
    1 -> VoxelState(this.key, this.oddIterationNumber, this.oddIterationMaterial, this.oddIterationTemperature)
    else -> throw InvalidSimulationState("Number modulo 2 can't have value other than 0 or 1 ")
}