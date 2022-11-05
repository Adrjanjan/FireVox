package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readTag
import java.io.IOException

class MainChunk private constructor(
    val contentSize: Int = 0,
    val overallChildrenByteSize: Int = 0,
    val packChunk: PackChunk?,
    val childrenChunks: MutableList<Pair<SizeChunk, VoxelsChunk>> = mutableListOf(),
    val paletteChunk: PaletteChunk?,
    val materialChunk: MaterialChunk?
) : Chunk(ChunkTags.TAG_MAIN) {

    companion object Builder {
        fun construct(input: LittleEndianDataInputStream): MainChunkBuilder {
            val chunkTag = input.readTag()
            if (chunkTag != ChunkTags.TAG_MAIN.tagValue) {
                throw IOException("Should be a ${ChunkTags.TAG_MAIN} tag here.")
            }
            return MainChunkBuilder(input.readInt(), input.readInt())
        }

        data class MainChunkBuilder(val contentSize: Int, val overallChildrenByteSize: Int) {
            var packChunk: PackChunk? = null
            var childrenChunks: MutableList<Pair<SizeChunk, VoxelsChunk>> = mutableListOf()
            var paletteChunk: PaletteChunk? = null
            var materialChunk: MaterialChunk? = null

            fun build(): MainChunk =
                MainChunk(contentSize, overallChildrenByteSize, packChunk, childrenChunks, paletteChunk, materialChunk)
        }
    }
}