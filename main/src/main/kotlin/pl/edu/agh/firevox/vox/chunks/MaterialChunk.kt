package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
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

data class MaterialV2(
    val type: MaterialType?,
    val metal: Float?,
    val rough: Float?,
    val spec: Float?,
    val ior: Float?,
    val att: Float?,
    val flux: Float?,
    val emit: Float?,
    val ldr: Float?,
    val trans: Float?,
    val alpha: Float?,
    val d: Float?,
    val sp: Float?,
    val g: Float?,
    val media: Float?,
)

enum class MaterialFlags(bit: Int) {
    METAL(1 shl 0),
    ROUGH(1 shl 1),
    SPEC(1 shl 2),
    IOR(1 shl 3),
    ATT(1 shl 4),
    FLUX(1 shl 5),
    EMIT(1 shl 6),
    LDR(1 shl 7),
    TRANS(1 shl 8),
    ALPHA(1 shl 9),
    D(1 shl 10),
    SP(1 shl 11),
    G(1 shl 12),
    MEDIA(1 shl 13),
}

enum class MaterialType {
    DIFFUSE,
    METAL,
    GLASS,
    EMIT,
    BLEND,
    MEDIA,
}