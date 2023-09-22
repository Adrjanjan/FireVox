package pl.edu.agh.firevox.shared.model.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.vox.readVoxDict

data class ShapeNodeChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_SHAPE_NODE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    override val nodeId: Int = input.readInt(),
    val nodeAttributes: Map<String, String> = input.readVoxDict(),
    val numOfModels: Int = input.readInt(),
    val models: List<ShapeModel> = readModels(input, numOfModels),
) : Chunk(), SceneNode

fun readModels(input: LittleEndianDataInputStream, numOfModels: Int): List<ShapeModel> =
    (0 until numOfModels).fold(mutableListOf()) { acc, _ ->
        acc.add(ShapeModel(input.readInt(), input.readVoxDict())); acc
    }

data class ShapeModel(val modelId: Int, private val attributes: Map<String, String>)