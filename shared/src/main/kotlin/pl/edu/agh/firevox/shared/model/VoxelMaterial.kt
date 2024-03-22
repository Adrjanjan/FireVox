package pl.edu.agh.firevox.shared.model

enum class VoxelMaterial(
    val colorId: Int,
) {
    AIR(0),
    SMOKE(1),
    WOOD(2),
    WOOD_BURNING(3),
    WOOD_BURNT(4),
    PLASTIC(5),
    PLASTIC_BURNING(6),
    PLASTIC_BURNT(7),
    TEXTILE(8),
    TEXTILE_BURNING(9),
    TEXTILE_BURNT(10),
    METAL(11),
    GLASS(12),
    CONCRETE(13),
    FLAME(14),
    WATER(15);

    companion object {
        fun fromId(value: Int): VoxelMaterial = VoxelMaterial.entries.firstOrNull { it.colorId == value }
            ?: AIR
//            ?: throw InvalidColorIdException(value)

    }
}

class InvalidColorIdException(colorId: Int) : Exception("The simulation model contains invalid color $colorId")
