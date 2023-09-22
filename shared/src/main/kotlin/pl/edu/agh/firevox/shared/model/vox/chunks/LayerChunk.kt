package pl.edu.agh.firevox.shared.model.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.vox.readVoxDict

data class LayerChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_LAYER,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    override val nodeId: Int = input.readInt(),
    val attributes: Map<String, String> = input.readVoxDict()
        .also { input.skip(4) }, // reservedId, skipped
) : Chunk(), SceneNode
