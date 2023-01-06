package pl.edu.agh.firevox.shared.model

data class VoxelDto(
    val x: Long,
    val y: Long,
    val z: Long,

    val energy: Double,
    val temperature: Double,
// TODO add other properties
)