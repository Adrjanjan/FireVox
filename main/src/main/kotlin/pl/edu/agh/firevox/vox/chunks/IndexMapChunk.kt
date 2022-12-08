package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

class IndexMapChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_INDEX_MAP,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val paletteSize: Int = size,
    val paletteIndexAssociation: List<Int> = readPaletteIndexAssociation(input, paletteSize),
) : Chunk()

private fun readPaletteIndexAssociation(input: LittleEndianDataInputStream, paletteSize: Int): MutableList<Int> =
    (0 until paletteSize).fold(mutableListOf()) { acc, _ -> acc.add(input.readUnsignedByte()); acc }