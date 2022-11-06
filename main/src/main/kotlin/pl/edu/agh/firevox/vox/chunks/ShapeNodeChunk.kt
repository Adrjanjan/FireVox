package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readVoxDict

data class ShapeNodeChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_SHAPE_NODE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val nodeId: Int = input.readInt(),
    val nodeAttributes: Map<String, String> = input.readVoxDict(),
    val numOfModels: Int = input.readInt(),
    val models: List<ShapeModel> = readModels(input, numOfModels),
) : Chunk()

fun readModels(input: LittleEndianDataInputStream, numOfModels: Int): List<ShapeModel> =
    (0..numOfModels).fold(mutableListOf()) { acc, _ ->
        acc.add(ShapeModel(input.readInt(), input.readVoxDict())); acc
    }

data class ShapeModel(val id: Int, private val attributes: Map<String, String>)