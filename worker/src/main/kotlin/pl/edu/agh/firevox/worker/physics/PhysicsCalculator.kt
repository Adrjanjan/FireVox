package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.Voxel
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*

@Service
class PhysicsCalculator(
    private val voxelRepository: CustomVoxelRepository,
) {

    fun calculate(voxel: Voxel) {
        val newMaterial = when(voxel.currentProperties.material){
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
    }
}