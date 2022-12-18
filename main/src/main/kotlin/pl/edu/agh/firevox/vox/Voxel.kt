package pl.edu.agh.firevox.vox

data class Voxel(val position: VoxelKey, var colorIndex: VoxelMaterialId)
data class VoxelKey(var x: Int, var y: Int, var z: Int)

typealias VoxelMaterialId = Int