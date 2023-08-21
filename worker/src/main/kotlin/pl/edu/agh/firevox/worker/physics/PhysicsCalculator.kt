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
        return when {
            voxel.isMadeOf("GLASS") -> conductionHeating(
                current.material,
                GLASS,
                GLASS_HEATED,
                GLASS_HOT,
                GLASS_VERY_HOT,
                neighbours
            )

            voxel.isMadeOf("METAL") -> conductionHeating(
                current.material,
                METAL,
                METAL_HEATED,
                METAL_HOT,
                METAL_VERY_HOT,
                neighbours
            )

            else -> throw InvalidSimulationState("Not GLASS or METAL in conduction transition")
        }
    }

    private fun conductionHeating(
        currentMaterial: VoxelMaterial,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial,
        neighbours: List<Voxel>
    ) = when (currentMaterial) {
        roomTemp -> roomTempConductionTransitions(neighbours, roomTemp, heated, hot)
        heated -> heatedConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
        hot -> hotConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
        veryHot -> veryHotConductionTransitions(neighbours, roomTemp, heated, hot, veryHot)
        else -> currentMaterial
    }

    private fun heatedConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = when {
        neighbours.all { it.isHighHeatSource() } -> veryHot
        heatingProbability(neighbours) > Random.nextDouble() -> hot
        coolingProbability(neighbours, 0.25, 0.0, 0.0, roomTemp, heated, hot) > Random.nextDouble() -> roomTemp
        else -> heated
    }

    private fun hotConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = when {
        neighbours.all { it.isHighHeatSource() } -> veryHot
        heatingProbability(neighbours) > Random.nextDouble() -> veryHot
        coolingProbability(neighbours, 0.5, 0.25, 0.0, roomTemp, heated, hot) > Random.nextDouble() -> roomTemp
        else -> hot
    }

    private fun veryHotConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial,
        veryHot: VoxelMaterial
    ) = when {
        coolingProbability(neighbours, 0.75, 0.50, 0.25, roomTemp, heated, hot) > Random.nextDouble() -> roomTemp
        else -> veryHot
    }

    private fun roomTempConductionTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        hot: VoxelMaterial
    ) = when {
        neighbours.all { it.isHighHeatSource() } -> hot
        heatingProbability(neighbours) > Random.nextDouble() -> heated
        else -> roomTemp
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

    private fun flammableMaterialsTransitions(voxel: Voxel): VoxelMaterial {
        val current = voxel.currentProperties
        val neighbours = voxelRepository.findNeighbors(
            voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, current.iterationNumber
        )
        return when {

            voxel.isMadeOf("WOOD") -> flammableMaterialsTransitions(
                current,
                WOOD,
                WOOD_HEATED,
                WOOD_BURNING,
                WOOD_BURNT,
                neighbours
            )

            voxel.isMadeOf("TEXTILE") -> flammableMaterialsTransitions(
                current,
                TEXTILE,
                TEXTILE_HEATED,
                TEXTILE_BURNING,
                TEXTILE_BURNT,
                neighbours
            )

            voxel.isMadeOf("PLASTIC") -> flammableMaterialsTransitions(
                current,
                PLASTIC,
                PLASTIC_HEATED,
                PLASTIC_BURNING,
                PLASTIC_BURNT,
                neighbours
            )

            else -> throw InvalidSimulationState("Not GLASS or METAL in conduction transition")
        }
    }

    private fun flammableMaterialsTransitions(
        state: StateProperties,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        burning: VoxelMaterial,
        burnt: VoxelMaterial,
        neighbours: List<Voxel>
    ) = when (state.material) {
        roomTemp -> roomTempBurningTransitions(neighbours, roomTemp, heated, burning)
        heated -> heatedBurningTransitions(neighbours, roomTemp, heated, burning)
        burning -> burningTransitions(state, burning, burnt)
        burnt -> burnt
        else -> state.material
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
    ) = when {
        neighbours.all { it.isHighHeatSource() } -> burning
        heatingProbability(neighbours) > Random.nextDouble() -> burning
        coolingProbability(neighbours, 0.25, 0.0, 0.0, roomTemp, heated, burning) > Random.nextDouble() -> roomTemp
        else -> heated
    }

    private fun roomTempBurningTransitions(
        neighbours: List<Voxel>,
        roomTemp: VoxelMaterial,
        heated: VoxelMaterial,
        burning: VoxelMaterial
    ) = when {
        neighbours.all { it.isHighHeatSource() } -> burning
        heatingProbability(neighbours) > Random.nextDouble() -> heated
        else -> roomTemp
    }

    private fun airTransitions(voxel: Voxel): VoxelMaterial {
        val current = voxel.currentProperties
        val neighbours = voxelRepository.findNeighbors(
            voxel.voxelKey, NeighbourhoodType.N_E_W_S_U_L_, current.iterationNumber
        )
        return when (current.material) {
            AIR -> airTransitions(voxel, neighbours)
            HALF_SMOKE -> halfSmokeTransitions(voxel, neighbours)
            FULL_SMOKE -> fullSmokeTransitions(voxel, neighbours)
            FLAME -> flameTransitions(voxel, neighbours)
            else -> throw InvalidSimulationState("Not GLASS or METAL in conduction transition")
        }
    }

    private fun flameTransitions(voxel: Voxel, neighbours: List<Voxel>): VoxelMaterial {
        return when {
            voxel.currentProperties.burningTick > 0 -> FLAME.also {
                voxel.currentProperties.burningTick -= 1
            }

            else -> FULL_SMOKE // flame ended
        }
    }

    private fun fullSmokeTransitions(voxel: Voxel, neighbours: List<Voxel>): VoxelMaterial {
        val lowerCell = neighbours.firstOrNull { it.isBelow(voxel) }
        val upperCell = neighbours.firstOrNull { it.isAbove(voxel) }
        val upperCanAbsorbSmoke = upperCell != null && upperCell.canAbsorbSmoke()
        val upperIsFull = upperCell != null && upperCell.isSolid() && upperCell.isMadeOf(FULL_SMOKE)
        val sidesCells = neighbours.filterNot { it == lowerCell || it == upperCell }
        val willLooseSmokeToTheSide = sidesCells.filter { it.canAbsorbSmoke() }.sumOf { 0.25 } > Random.nextDouble()

        return when {
            lowerCell?.isMadeOf(AIR) ?: false && upperCanAbsorbSmoke -> HALF_SMOKE
            lowerCell?.isSmokeSource() ?: false && upperCanAbsorbSmoke -> FULL_SMOKE
            upperIsFull && willLooseSmokeToTheSide && lowerCell?.isSmokeSource() == true -> FULL_SMOKE
            upperIsFull && willLooseSmokeToTheSide && lowerCell?.isSmokeSource() == false -> HALF_SMOKE
            else -> FULL_SMOKE
        }
    }

    private fun halfSmokeTransitions(voxel: Voxel, neighbours: List<Voxel>): VoxelMaterial {
        val lowerCell = neighbours.firstOrNull { it.isBelow(voxel) }
        val upperCell = neighbours.firstOrNull { it.isAbove(voxel) }
        val upperCanAbsorbSmoke = upperCell != null && upperCell.canAbsorbSmoke()

        return when {
            lowerCell != null && !lowerCell.isSmokeSource() && upperCanAbsorbSmoke -> AIR
            lowerCell != null && !lowerCell.isSmokeSource() && !upperCanAbsorbSmoke -> HALF_SMOKE
            lowerCell != null && lowerCell.isSmokeSource() && upperCanAbsorbSmoke -> HALF_SMOKE
            lowerCell != null && lowerCell.isSmokeSource() && !upperCanAbsorbSmoke -> FULL_SMOKE
            lowerCell == null && upperCanAbsorbSmoke -> AIR
            else -> HALF_SMOKE
        }
    }

    private fun airTransitions(voxel: Voxel, neighbours: List<Voxel>): VoxelMaterial {
        val lowerCell = neighbours.firstOrNull { it.isBelow(voxel) }
        val upperCell = neighbours.firstOrNull { it.isAbove(voxel) }

        if (lowerCell != null && lowerCell.isSmokeSource()) return HALF_SMOKE

        val sidesAreSmokeSource = neighbours.filterNot { it == upperCell || it == lowerCell }
            .filter { it.isSmokeSource() }
            .sumOf { 0.25 } > Random.nextDouble()

        if (upperCell == null || upperCell.isSolid() || upperCell.isMadeOf(FULL_SMOKE)) {
            if (sidesAreSmokeSource) return HALF_SMOKE
            return AIR
        }
        if (lowerCell == null) return AIR

        if (sidesAreSmokeSource && lowerCell.isSmokeSource()) return FULL_SMOKE

        // propagate flame
        if (lowerCell.isMadeOf(FLAME)) return FLAME.also {
            voxel.currentProperties.burningTick = lowerCell.nextProperties.burningTick - 1
        }

        // start flame
        if (lowerCell.isBurning()) return FLAME.also {
            voxel.nextProperties.burningTick = lowerCell.currentProperties.material.burningDuration
        }

        return AIR
    }

    private fun Voxel.isMadeOf(material: VoxelMaterial) = currentProperties.material == material

    private fun Voxel.isMadeOf(material: String) = currentProperties.material.toString().contains(material)

    private fun Voxel.canAbsorbSmoke() = this.currentProperties.material in listOf(HALF_SMOKE, AIR)

    private fun Voxel.isSolid() = this.currentProperties.material in listOf(
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

    private fun Voxel.isSmokeSource() = this.currentProperties.material in listOf(
        HALF_SMOKE,
        FULL_SMOKE,
        WOOD_HEATED,
        TEXTILE_HEATED,
        WOOD_BURNING,
        TEXTILE_BURNING,
    )

    private fun Voxel.isBurning() = this.currentProperties.material in listOf(
        WOOD_BURNING,
        TEXTILE_BURNING,
    )

    private fun Voxel.isLowHeatSource() = this.currentProperties.material in listOf(
        WOOD_HEATED,
        PLASTIC_HEATED,
        TEXTILE_HEATED,
        GLASS_HEATED,
        METAL_HEATED,
        METAL_HOT,
        GLASS_HOT,
    )

    private fun Voxel.isHighHeatSource() = this.currentProperties.material in listOf(
        WOOD_BURNING,
        PLASTIC_BURNING,
        TEXTILE_BURNING,
        GLASS_VERY_HOT,
        METAL_VERY_HOT,
        FLAME
    )

}