package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readVoxDict

data class LayerChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_LAYER,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val id: Int = input.readInt(),
    val attributes: Map<String, String> = input.readVoxDict().also { input.skip(4) },
) : Chunk()
