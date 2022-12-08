package pl.edu.agh.firevox.simulation

import pl.edu.agh.firevox.vox.ParsedVoxFile
import pl.edu.agh.firevox.vox.Voxel

data class SimulationScene(
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
    val voxels: List<Voxel>,
) {
    constructor(parsedVoxFile: ParsedVoxFile) : this(
        parsedVoxFile.voxels.maxOf { it.x },
        parsedVoxFile.voxels.maxOf { it.y },
        parsedVoxFile.voxels.maxOf { it.z },
        parsedVoxFile.voxels
    )

    // TODO should I include render options here or save them during parsing?
}