package pl.edu.agh.firevox.shared.model.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.vox.readVoxDict

class RenderObjectsChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_RENDER_OBJECTS,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val attributes: Map<String, String> = input.readVoxDict(),
) : Chunk()