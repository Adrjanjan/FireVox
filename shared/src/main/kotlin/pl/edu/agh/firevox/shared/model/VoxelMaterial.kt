package pl.edu.agh.firevox.shared.model

enum class VoxelMaterial(
    val colorId: Int,
) {
    AIR(0),
    SMOKE(1),
    WOOD(3),
    WOOD_HEATED(4),
    WOOD_BURNING(5),
    WOOD_BURNT(6),
    PLASTIC(7),
    PLASTIC_HEATED(8),
    PLASTIC_BURNING(9),
    PLASTIC_BURNT(10),
    TEXTILE(11),
    TEXTILE_HEATED(12),
    TEXTILE_BURNING(13),
    TEXTILE_BURNT(14),
    METAL(15),
    METAL_HEATED(16),
    METAL_HOT(17),
    METAL_VERY_HOT(18),
    GLASS(19),
    GLASS_HEATED(20),
    GLASS_HOT(21),
    GLASS_VERY_HOT(21),
    CONCRETE(23),
    FLAME(24),
    WATER(25);

    companion object {
        fun fromId(value: Int): VoxelMaterial = VoxelMaterial.entries.firstOrNull { it.colorId == value }
            ?: CONCRETE
//            ?: throw InvalidColorIdException(value)

    }
}

class InvalidColorIdException(colorId: Int) : Exception("The simulation model contains invalid color $colorId")
