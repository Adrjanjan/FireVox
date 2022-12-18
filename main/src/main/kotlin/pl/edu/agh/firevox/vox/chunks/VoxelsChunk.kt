package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.vox.Voxel
import pl.edu.agh.firevox.vox.VoxelKey
import pl.edu.agh.firevox.vox.VoxelMaterialId
import pl.edu.agh.firevox.vox.readTag
import java.io.IOException

data class VoxelsChunk(
    val input: LittleEndianDataInputStream,
    override var tag: ChunkTags = ChunkTags.TAG_XYZI,
    override val size: Int = input.readInt(),
    override val childSize: Int = input.readInt(),
    val numberOfVoxels: Int = input.readInt(),
    val voxels: Map<VoxelKey, VoxelMaterialId> = readVoxels(numberOfVoxels, input)
) : Chunk()

private fun readVoxels(
    numVoxels: Int,
    input: LittleEndianDataInputStream
): MutableMap<VoxelKey, VoxelMaterialId>  {
    val voxels = mutableMapOf<VoxelKey, VoxelMaterialId>()
    for (i in 0 until numVoxels) {
        val x = input.readUnsignedByte()
        val y = input.readUnsignedByte()
        val z = input.readUnsignedByte()
        val colorIndex = input.readUnsignedByte()
        voxels[VoxelKey(x, y, z)] =  colorIndex
//        println("Voxel x=$x, y=$y, z=$z, p=$colorIndex")
    }
    return voxels
}
