package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readTag
import java.io.IOException

data class SizeChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_SIZE,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val sizeX: Int = input.readInt(),
    val sizeY: Int = input.readInt(),
    val sizeZ: Int = input.readInt(),
) : Chunk()