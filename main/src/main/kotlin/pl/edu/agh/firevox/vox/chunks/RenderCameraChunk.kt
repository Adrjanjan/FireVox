package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readVoxDict

class RenderCameraChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_RENDER_CAMERA,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val id: Int = input.readInt(),
    val attributes: Map<String, String> = input.readVoxDict(),
) : Chunk()

enum class CamMode(val bit: Int) {
    PERSPECTIVE(0),
    FREE(1),
    PANO(2),
    ORTHOGRAPHIC(3),
    ISOMETRIC(4),
    UNKNOWN(5),
};