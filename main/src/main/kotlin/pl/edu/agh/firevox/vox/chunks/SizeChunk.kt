package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.readTag
import java.io.IOException

data class SizeChunk(val sizeX: Int, val sizeY: Int, val sizeZ: Int) : Chunk(ChunkTags.TAG_SIZE) {

    companion object {
        fun construct(input: LittleEndianDataInputStream, skipTag: Boolean): SizeChunk {
            if (!skipTag) {
                val tag = input.readTag()
                if (tag != ChunkTags.TAG_SIZE.tagValue) {
                    throw IOException("Should be a ${ChunkTags.TAG_SIZE.tagValue} tag here.")
                }
            }
            input.skipBytes(8)
            val sizeX = input.readInt()
            val sizeY = input.readInt()
            val sizeZ = input.readInt()
            return SizeChunk(sizeX, sizeY, sizeZ)
        }

    }
}