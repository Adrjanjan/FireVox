package pl.edu.agh.firevox.model

data class ModelDescriptionDto(
    val outputName: String,
    val parentModel: SingleModelDto,
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
