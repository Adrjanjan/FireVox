package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.chunks.ChunkTags.*

data class MainChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = TAG_MAIN,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
) : Chunk()
