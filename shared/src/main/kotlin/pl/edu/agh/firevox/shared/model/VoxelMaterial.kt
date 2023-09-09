package pl.edu.agh.firevox.shared.model

enum class VoxelMaterial(
    val colorId: Int,
) {
    AIR(0),
    SMOKE(1),
    WOOD(2),
    WOOD_HEATED(3),
    WOOD_BURNING(4),
    WOOD_BURNT(5),
    PLASTIC(6),
    PLASTIC_HEATED(7),
    PLASTIC_BURNING(8),
    PLASTIC_BURNT(9),
    TEXTILE(10),
    TEXTILE_HEATED(11),
    TEXTILE_BURNING(12),
    TEXTILE_BURNT(13),
    METAL(14),
    METAL_HEATED(15),
    METAL_HOT(16),
    METAL_VERY_HOT(17),
    GLASS(18),
    GLASS_HEATED(19),
    GLASS_HOT(20),
    GLASS_VERY_HOT(21),
    CONCRETE(22),
    FLAME(23),
    WATER(24);

    companion object {
        fun fromId(value: Int): VoxelMaterial = VoxelMaterial.entries.firstOrNull { it.colorId == value }
            ?: AIR
//            ?: throw InvalidColorIdException(value)

    }
}

class InvalidColorIdException(colorId: Int) : Exception("The simulation model contains invalid color $colorId")
