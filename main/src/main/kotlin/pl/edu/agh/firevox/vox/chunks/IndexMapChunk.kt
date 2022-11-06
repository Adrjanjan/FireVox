package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

data class IndexMapChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_LAYER,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val paletteSize: Int = 256,
    val colorNames: List<Int> = readPaletteIndexAssociation(input, paletteSize),
) : Chunk()

private fun readPaletteIndexAssociation(input: LittleEndianDataInputStream, paletteSize: Int): MutableList<Int> =
    (1..paletteSize).fold(mutableListOf()) { acc, _ ->
        acc.add(input.readInt()); acc
    }