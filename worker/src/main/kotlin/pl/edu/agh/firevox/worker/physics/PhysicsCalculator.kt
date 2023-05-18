package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.StateProperties
import pl.edu.agh.firevox.shared.model.Voxel
import pl.edu.agh.firevox.shared.model.VoxelMaterial
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*

@Service
class PhysicsCalculator(
    private val voxelRepository: CustomVoxelRepository,
) {

    fun calculate(voxel: Voxel) {
        val newMaterial = when (voxel.currentProperties.material) {
            AIR, HALF_SMOKE, FULL_SMOKE, FLAME
            -> airTransitions(voxel)

            WOOD, WOOD_HEATED, WOOD_BURNING, WOOD_BURNT, PLASTIC, PLASTIC_BURNING,
            PLASTIC_BURNT, TEXTILE, TEXTILE_BURNING, TEXTILE_BURNT
            -> burningTransitions(voxel)

            METAL, METAL_HEATED, METAL_HOT, METAL_VERY_HOT, GLASS, GLASS_HEATED, GLASS_HOT, GLASS_VERY_HOT
            -> conductionTransitions(voxel)

            CONCRETE
            -> concreteTransitions(voxel)

        }
        val newIteration = voxel.currentProperties.iterationNumber + 1
        voxel.nextProperties = StateProperties(newIteration, newMaterial)
    }

    private fun concreteTransitions(voxel: Voxel): VoxelMaterial {
        TODO("Not yet implemented")
    }

    private fun conductionTransitions(voxel: Voxel): VoxelMaterial {
        TODO("Not yet implemented")
    }

    private fun burningTransitions(voxel: Voxel): VoxelMaterial {
        TODO("Not yet implemented")
    }

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
        GLASS_HEATED,
        METAL_HEATED,
        METAL_HOT,
        GLASS_HOT
    )

    fun Voxel.isHighHeatSource() = this.currentProperties.material in listOf(
        WOOD_BURNING,
        PLASTIC_BURNING,
        TEXTILE_BURNING,
        GLASS_VERY_HOT,
        METAL_VERY_HOT,
        FLAME
    )

    fun Voxel.isHeatSource() = this.currentProperties.material in listOf(

    )
}