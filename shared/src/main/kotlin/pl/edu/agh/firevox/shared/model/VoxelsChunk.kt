package pl.edu.agh.firevox.shared.model

import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView

data class VoxelsChunk(
    val startingVoxel: VoxelKey,
    val endingVoxel: VoxelKey,
    val model: SimulationSizeView,
) {
    lateinit var voxels: Array<Array<Array<Voxel?>>>

    fun neighbours(key: VoxelKey, neighbourhoodType: NeighbourhoodType): Pair<MutableList<Voxel>, Set<VoxelKey>> {
        val result = neighbourhoodType.keyMapping.map { key.copy(x = key.x + it.x, y = key.y + it.y, z = key.z + it.z) }
            .filter(::verifyInbound).associateWith { voxels[it - startingVoxel] }
        val notNullFound = result.values.filterNotNull().toMutableList()
        val keysToFill = result.filter { it.value == null }.keys
        return notNullFound to keysToFill
    }

    private fun verifyInbound(k: VoxelKey) = if (k.x < 0 || k.y < 0 || k.z < 0) false
    else (k.x <= endingVoxel.x - startingVoxel.x && k.y <= endingVoxel.y - startingVoxel.y && k.z <= endingVoxel.z - startingVoxel.z)

    fun flatten(): List<Voxel> {
        val flatten: List<Array<Voxel?>> = voxels.flatten()
        return flatten.flatMap { it.filterNotNull() }.toList()
    }
}

private inline operator fun Array<Array<Array<Voxel?>>>.get(key: VoxelKey): Voxel? = this[key.x][key.y][key.z]
private inline operator fun Array<Array<Array<Voxel?>>>.set(key: VoxelKey, value: Voxel) {
    this[key.x][key.y][key.z] = value
}