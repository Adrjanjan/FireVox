package pl.edu.agh.firevox.shareddto.model

data class VoxelDto(
    val x: Long,
    val y: Long,
    val z: Long,

    val energy: Double,
    val temperature: Double,

)