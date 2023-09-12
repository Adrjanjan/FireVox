package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.simulation.Colour

data class PaletteChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_RGBA,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val colors: List<Colour> = readPalette(input)
) : Chunk()

private fun readPalette(input: LittleEndianDataInputStream) = (0..255).map {
    val r = input.readUnsignedByte()
    val g = input.readUnsignedByte()
    val b = input.readUnsignedByte()
    val a = input.readUnsignedByte()
    Colour(it, r, g, b, a)
}