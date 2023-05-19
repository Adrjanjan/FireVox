package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*
import pl.edu.agh.firevox.worker.service.InvalidSimulationState
import kotlin.random.Random

@Service
class PhysicsCalculator(
    private val voxelRepository: CustomVoxelRepository,
) {

    fun calculate(voxel: Voxel) {
        val newMaterial = when (voxel.currentProperties.material) {
            AIR, HALF_SMOKE, FULL_SMOKE, FLAME
            -> airTransitions(voxel)

            WOOD, WOOD_HEATED, WOOD_BURNING, WOOD_BURNT, PLASTIC, PLASTIC_HEATED, PLASTIC_BURNING,
            PLASTIC_BURNT, TEXTILE, TEXTILE_HEATED, TEXTILE_BURNING, TEXTILE_BURNT
            -> flammableMaterialsTransitions(voxel)

            METAL, METAL_HEATED, METAL_HOT, METAL_VERY_HOT, GLASS, GLASS_HEATED, GLASS_HOT, GLASS_VERY_HOT
            -> conductionTransitions(voxel)

            CONCRETE
            -> concreteTransitions(voxel)


        }
        val newIteration = voxel.currentProperties.iterationNumber + 1
        voxel.nextProperties = StateProperties(newIteration, newMaterial)
    }

    private fun concreteTransitions(voxel: Voxel) = CONCRETE

    private fun conductionTransitions(voxel: Voxel): VoxelMaterial {
        val current = voxel.currentProperties
        val neighbours = voxelRepository.findNeighbors(
            voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, current.iterationNumber
        )
        return if (current.material.toString().contains("GLASS")) {
            conductionHeating(current.material, GLASS, GLASS_HEATED, GLASS_HOT, GLASS_VERY_HOT, neighbours)
        } else if (current.material.toString().contains("METAL")) {
            conductionHeating(current.material, METAL, METAL_HEATED, METAL_HOT, METAL_VERY_HOT, neighbours)
        } else throw InvalidSimulationState("Not GLASS or METAL in conduction transition")
    }

    fun conductionHeating(
        currentMaterial: VoxelMaterial,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial,
        neighbours: List<Voxel>
    ): VoxelMaterial {
        return when (currentMaterial) {
            roomTemp -> roomTempConductionTransitions(neighbours, roomTemp, heated, hot)
            heated -> heatedConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
            hot -> hotConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
            veryHot -> veryHotConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
            else -> currentMaterial
        }
    }

    private fun heatedConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = if (neighbours.all { it.isHighHeatSource() }) veryHot
    else if (heatingProbability(neighbours) > Random.nextDouble()) hot
    else if (coolingProbability(neighbours, 0.25, 0.0, 0.0, roomTemp, heated, hot) > Random.nextDouble()) roomTemp
    else heated

    private fun hotConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = if (neighbours.all { it.isHighHeatSource() }) veryHot
    else if (heatingProbability(neighbours) > Random.nextDouble()) veryHot
    else if (coolingProbability(neighbours, 0.5, 0.25, 0.0, roomTemp, heated, hot) > Random.nextDouble()) roomTemp
    else hot

    private fun veryHotConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = if (coolingProbability(neighbours, 0.75, 0.50, 0.25, roomTemp, heated, hot) > Random.nextDouble()) roomTemp
    else veryHot

    private fun roomTempConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial
    ) = if (neighbours.all { it.isHighHeatSource() }) hot else {
        if (heatingProbability(neighbours) > Random.nextDouble()) heated
        else roomTemp
    }

    private fun heatingProbability(
        neighbours: List<Voxel>,
        lowHeatIncrement: Double = 0.25,
        highHeatIncrement: Double = 0.5
    ) = neighbours.sumOf {
        when {
            it.isLowHeatSource() -> lowHeatIncrement
            it.isHighHeatSource() -> highHeatIncrement
            else -> 0.0
        }
    }

    private fun coolingProbability(
        neighbours: List<Voxel>,
        roomTempIncrement: Double = 0.75,
        heatedIncrement: Double = 0.5,
        hotIncrement: Double = 0.25,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial
    ) = neighbours.sumOf {
        when (it.currentProperties.material) {
            roomTemp -> roomTempIncrement
            heated -> heatedIncrement
            hot -> hotIncrement
            else -> 0.0
        }
    }

    fun flammableMaterialsTransitions(voxel: Voxel): VoxelMaterial {
        val current = voxel.currentProperties
        val neighbours = voxelRepository.findNeighbors(
            voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, current.iterationNumber
        )
        return if (current.material.toString().contains("WOOD")) {
            flammableMaterialsTransitions(current, WOOD, WOOD_HEATED, WOOD_BURNING, WOOD_BURNT, neighbours)
        } else if (current.material.toString().contains("TEXTILE")) {
            flammableMaterialsTransitions(current, TEXTILE, TEXTILE_HEATED, TEXTILE_BURNING, TEXTILE_BURNT, neighbours)
        } else if (current.material.toString().contains("PLASTIC")) {
            flammableMaterialsTransitions(current, PLASTIC, PLASTIC_HEATED, PLASTIC_BURNING, PLASTIC_BURNT, neighbours)
        } else throw InvalidSimulationState("Not GLASS or METAL in conduction transition")
    }

    fun flammableMaterialsTransitions(
        state: StateProperties,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        burning: VoxelMaterial,
        burnt: VoxelMaterial,
        neighbours: List<Voxel>
    ): VoxelMaterial {
        return when (state.material) {
            roomTemp -> roomTempBurningTransitions(neighbours, roomTemp, heated, burning)
            heated -> heatedBurningTransitions(neighbours, roomTemp, heated, burning)
            burning -> burningTransitions(state, burning, burnt)
            burnt -> burnt
            else -> state.material
        }
    }

    private fun burningTransitions(
        state: StateProperties,
        burning: VoxelMaterial,
        burnt: VoxelMaterial,
    ) = if (burning.burningDuration > state.burningTick) {
        state.burningTick = state.burningTick++
        burning
    } else burnt

    private fun heatedBurningTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        burning: VoxelMaterial
    ) = if (neighbours.all { it.isHighHeatSource() }) burning
    else if (heatingProbability(neighbours) > Random.nextDouble()) burning
    else if (coolingProbability(neighbours, 0.25, 0.0, 0.0, roomTemp, heated, burning) > Random.nextDouble()) roomTemp
    else heated

    private fun roomTempBurningTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        burning: VoxelMaterial
    ) = if (neighbours.all { it.isHighHeatSource() }) burning
    else if (heatingProbability(neighbours) > Random.nextDouble()) heated
    else roomTemp


    private fun airTransitions(voxel: Voxel): VoxelMaterial {
        TODO("Not yet implemented")
    }

    fun Voxel.isSolid() = this.currentProperties.material in listOf(
        WOOD,
        WOOD_HEATED,
        WOOD_BURNING,
        WOOD_BURNT,
        PLASTIC,
        PLASTIC_BURNING,
        PLASTIC_BURNT,
        TEXTILE,
        TEXTILE_BURNING,
        TEXTILE_BURNT,
        METAL,
        METAL_HEATED,
        METAL_HOT,
        METAL_VERY_HOT,
        GLASS,
        GLASS_HEATED,
        GLASS_HOT,
        GLASS_VERY_HOT,
        CONCRETE,
    )

    fun Voxel.isSmokeSource() = this.currentProperties.material in listOf(
        HALF_SMOKE,
        FULL_SMOKE,
        WOOD_HEATED,
        FLAME
    )

    fun Voxel.isLowHeatSource() = this.currentProperties.material in listOf(
        WOOD_HEATED,
        PLASTIC_HEATED,
        TEXTILE_HEATED,
        GLASS_HEATED,
        METAL_HEATED,
        METAL_HOT,
        GLASS_HOT,
    )

    fun Voxel.isHighHeatSource() = this.currentProperties.material in listOf(
        WOOD_BURNING,
        PLASTIC_BURNING,
        TEXTILE_BURNING,
        GLASS_VERY_HOT,
        METAL_VERY_HOT,
        FLAME
    )

    fun Voxel.isHeatSource() = isLowHeatSource() || isHighHeatSource()
}