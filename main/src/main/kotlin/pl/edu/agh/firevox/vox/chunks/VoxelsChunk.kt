package pl.edu.agh.firevox.vox.chunks

import com.google.common.io.LittleEndianDataInputStream
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.vox.VoxelMaterialId

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
        val key = VoxelKey(
            x = input.readUnsignedByte(),
            y = input.readUnsignedByte(),
            z = input.readUnsignedByte()
        )
        val colorIndex = input.readUnsignedByte()
        voxels[key] =  colorIndex
//        println("Voxel[$key] = $colorIndex")
    }
    return voxels
}
