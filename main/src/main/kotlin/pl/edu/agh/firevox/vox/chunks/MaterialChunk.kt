package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.MaterialType
import pl.edu.agh.firevox.vox.readVoxDict

data class MaterialChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_MATL,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val id: Int = input.readInt(),
    val properties: Map<String, String> = input.readVoxDict(),
) : Chunk()

data class Material(
    val type: MaterialType?,
    val weight: Float?,
    val rough: Float?,
    val spec: Float?,
    val ior: Float?,
    val att: Float?,
    val flux: Float?,
    val plastic: Int?,
)

enum class MaterialType{
    DIFFUSE,
    METAL,
    GLASS,
    EMIT
}