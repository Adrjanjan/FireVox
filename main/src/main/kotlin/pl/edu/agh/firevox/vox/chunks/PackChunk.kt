package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

data class PackChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_PACK,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val numberOfChunks: Int = input.readInt(),
) : Chunk()