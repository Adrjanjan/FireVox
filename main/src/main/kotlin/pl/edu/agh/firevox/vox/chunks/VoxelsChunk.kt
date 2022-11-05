package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.Voxel
import pl.edu.agh.firevox.vox.readTag
import java.io.IOException

data class VoxelsChunk(val numberOfVoxels: Int, val voxels: List<Voxel>) : Chunk(ChunkTags.TAG_XYZI) {

    companion object {
        fun construct(input: LittleEndianDataInputStream): VoxelsChunk {
            // Voxel data chunk
            val xyziTag = input.readTag()
            if (xyziTag != ChunkTags.TAG_XYZI.tagValue) {
                throw IOException("Should be a ${ChunkTags.TAG_XYZI.tagValue} tag here.")
            }
            input.skipBytes(8)
            val numVoxels = input.readInt()
            val voxels = mutableListOf<Voxel>()
            for (i in 0 until numVoxels) {
                val x = input.readUnsignedByte()
                val y = input.readUnsignedByte()
                val z = input.readUnsignedByte()
                val colorIndex = input.readUnsignedByte()
                voxels.add(Voxel(x, y, z, colorIndex))
                println("Voxel x=$x, y=$y, z=$z, p=$colorIndex")
            }
            return VoxelsChunk(numVoxels, voxels)
        }
    }
}