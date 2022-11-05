package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream

data class PackChunk(val numberOfChunks: Int = 0) : Chunk(ChunkTags.TAG_PACK) {

    companion object {
        fun construct(input: LittleEndianDataInputStream): PackChunk {
            // tag was read in read method
            input.skipBytes(8)
            return PackChunk(input.readInt())
        }

    }
}