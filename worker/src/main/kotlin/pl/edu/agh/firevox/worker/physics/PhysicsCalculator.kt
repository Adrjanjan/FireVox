package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.StateProperties
import pl.edu.agh.firevox.shared.model.Voxel
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*

@Service
class PhysicsCalculator(
    private val voxelRepository: CustomVoxelRepository,
) {

    fun calculate(voxel: Voxel) {
        val newMaterial = when (voxel.currentProperties.material) {
            AIR -> TODO()
            HALF_SMOKE -> TODO()
            FULL_SMOKE -> TODO()
            WOOD -> TODO()
            WOOD_HEATED -> TODO()
            WOOD_BURNING -> TODO()
            WOOD_BURNT -> TODO()
            PLASTIC -> TODO()
            PLASTIC_BURNING -> TODO()
            PLASTIC_BURNT -> TODO()
            TEXTILE -> TODO()
            TEXTILE_BURNING -> TODO()
            TEXTILE_BURNT -> TODO()
            METAL -> TODO()
            METAL_HEATED -> TODO()
            METAL_HOT -> TODO()
            METAL_VERY_HOT -> TODO()
            GLASS -> TODO()
            GLASS_HEATED -> TODO()
            GLASS_HOT -> TODO()
            GLASS_VERY_HOT -> TODO()
            CONCRETE -> TODO()
            FLAME -> TODO()
        }
        val newIteration = voxel.currentProperties.iterationNumber + 1
        voxel.nextProperties = StateProperties(newIteration, newMaterial)
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