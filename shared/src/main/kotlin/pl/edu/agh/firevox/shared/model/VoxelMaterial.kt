package pl.edu.agh.firevox.shared.model

enum class VoxelMaterial(
    val colorId: Int,
    val burningDuration: Int,
) {
    AIR(0, 0),
    HALF_SMOKE(1, 0),
    FULL_SMOKE(2, 0),
    WOOD(3, 0),
    WOOD_HEATED(4, 0),
    WOOD_BURNING(5, 10),
    WOOD_BURNT(6, 0),
    PLASTIC(7, 0),
    PLASTIC_HEATED(8, 0),
    PLASTIC_BURNING(9, 5),
    PLASTIC_BURNT(10, 0),
    TEXTILE(11, 0),
    TEXTILE_HEATED(12, 0),
    TEXTILE_BURNING(13, 3),
    TEXTILE_BURNT(14, 0),
    METAL(15, 0),
    METAL_HEATED(16, 0),
    METAL_HOT(17, 0),
    METAL_VERY_HOT(18, 0),
    GLASS(19, 0),
    GLASS_HEATED(20, 0),
    GLASS_HOT(21, 0),
    GLASS_VERY_HOT(21, 0),
    CONCRETE(23, 0),
    FLAME(24, 0);

    companion object {
        fun fromId(value: Int): VoxelMaterial = VoxelMaterial.entries.firstOrNull { it.colorId == value }
            ?: CONCRETE
//            ?: throw InvalidColorIdException(value)

    }
}

class InvalidColorIdException(colorId: Int) : Exception("The simulation model contains invalid color $colorId")
