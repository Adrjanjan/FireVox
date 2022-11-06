package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readVoxDict

data class GroupNodeChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_GROUP_NODE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val nodeId: Int = input.readInt(),
    val nodeAttributes: Map<String, String> = input.readVoxDict(),
    val numOfChildrenNodes: Int = input.readInt(),
    val childNodeIds: List<Int> = readChildNodeIds(input, numOfChildrenNodes),
) : Chunk()

fun readChildNodeIds(input: LittleEndianDataInputStream, numOfChildrenNodes: Int): List<Int> =
    (0..numOfChildrenNodes).fold(mutableListOf()) { acc, _ -> acc.add(input.readInt()); acc}

