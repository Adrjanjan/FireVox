package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

data class PaletteChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_MATL,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val colors: MutableList<Long> = readPalette(input)
) : Chunk()

private fun readPalette(input: LittleEndianDataInputStream): MutableList<Long> {
    val colors = mutableListOf<Long>(0)
    for (i in 1..255) {
        val r = input.readUnsignedByte()
        val g = input.readUnsignedByte()
        val b = input.readUnsignedByte()
        val a = input.readUnsignedByte()
        colors.add(fromRgba(r, g, b, a))
    }
    return colors
}

private fun fromRgba(r: Int, g: Int, b: Int, a: Int) = String.format("%02x%02x%02x%02x", r, g, b, a).toLong(16)
