package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

data class PaletteChunk(val colors: MutableList<Long>) : Chunk(ChunkTags.TAG_RGBA) {

    companion object {
        fun construct(input: LittleEndianDataInputStream): PaletteChunk {
            // Palette chunk
            val colors = mutableListOf<Long>(0)
            input.skipBytes(8)
            for (i in 1..255) {
                val r = input.readUnsignedByte()
                val g = input.readUnsignedByte()
                val b = input.readUnsignedByte()
                val a = input.readUnsignedByte()
                colors.add(fromRgba(r, g, b, a))
            }
            return PaletteChunk(colors)
        }

        private fun fromRgba(r: Int, g: Int, b: Int, a: Int) = String.format("%02x%02x%02x%02x", r, g, b, a).toLong(16)
    }
}