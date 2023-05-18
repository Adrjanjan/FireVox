package pl.edu.agh.firevox.shared.model

import javax.persistence.Entity
import javax.persistence.Id

enum class VoxelMaterial(
    val colorId: Int
) {
    AIR(0),
    HALF_SMOKE(1),
    FULL_SMOKE(2),
    WOOD(3),
    WOOD_HEATED(4),
    WOOD_BURNING(5),
    WOOD_BURNT(6),
    PLASTIC(7),
    PLASTIC_BURNING(8),
    PLASTIC_BURNT(9),
    TEXTILE(10),
    TEXTILE_BURNING(11),
    TEXTILE_BURNT(12),
    METAL(13),
    METAL_HEATED(14),
    METAL_HOT(15),
    METAL_VERY_HOT(16),
    GLASS(17),
    GLASS_HEATED(18),
    GLASS_HOT(19),
    GLASS_VERY_HOT(20),
    CONCRETE(8),
    FLAME(9),
}
