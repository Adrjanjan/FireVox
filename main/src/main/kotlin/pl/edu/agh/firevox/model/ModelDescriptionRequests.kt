package pl.edu.agh.firevox.model

import pl.edu.agh.firevox.shared.model.VoxelKey

data class ModelDescriptionDto(
    val outputName: String,
    val parentModel: SingleModelDto,
    val pointsOfPlanesForRadiation: PointsToNormals? = null
)

data class PointsToNormals(
    val points: List<Pair<VoxelKey, VoxelKey>>
)

data class SingleModelDto(
    val name: String,
    val childModels: List<SingleModelDto> = listOf(),
    val scale: Int = 1,
    val positionX: Int? = 0,
    val positionY: Int? = 0,
    val positionZ: Int? = 0,
    val centerX: Boolean? = false,
    val centerY: Boolean? = false,
    val centerZ: Boolean? = false,
    val flipX: Boolean? = false,
    val flipY: Boolean? = false,
    val flipZ: Boolean? = false,
    val rotateX: Int? = 0,
    val rotateY: Int? = 0,
    val rotateZ: Int? = 0,
)
