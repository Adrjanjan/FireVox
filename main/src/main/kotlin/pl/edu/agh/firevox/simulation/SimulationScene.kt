package pl.edu.agh.firevox.simulation

import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelMaterial
import pl.edu.agh.firevox.vox.ParsedVoxFile

data class SimulationScene(
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    val voxels: Map<VoxelKey, VoxelMaterial>,
) {
    constructor(parsedVoxFile: ParsedVoxFile) : this(
        parsedVoxFile.voxels.maxOf { it.key.x },
        parsedVoxFile.voxels.maxOf { it.key.y },
        parsedVoxFile.voxels.maxOf { it.key.z },
        parsedVoxFile.voxels
    )

    // TODO should I include render options here or save them during parsing?
}