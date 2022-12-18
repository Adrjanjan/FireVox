package pl.edu.agh.firevox.simulation

import pl.edu.agh.firevox.vox.ParsedVoxFile
import pl.edu.agh.firevox.vox.VoxelKey
import pl.edu.agh.firevox.vox.VoxelMaterialId

data class SimulationScene(
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    val voxels: Map<VoxelKey, VoxelMaterialId>,
) {
    constructor(parsedVoxFile: ParsedVoxFile) : this(
        parsedVoxFile.voxels.maxOf { it.key.x },
        parsedVoxFile.voxels.maxOf { it.key.y },
        parsedVoxFile.voxels.maxOf { it.key.z },
        parsedVoxFile.voxels
    )

    // TODO should I include render options here or save them during parsing?
}