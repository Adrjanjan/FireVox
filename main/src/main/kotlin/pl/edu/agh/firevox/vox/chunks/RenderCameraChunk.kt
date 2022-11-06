package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readVoxDict

class RenderCameraChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_RENDER_OBJECTS,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val id: Int = input.readInt(),
    val attributes: Map<String, String> = input.readVoxDict(),
) : Chunk()