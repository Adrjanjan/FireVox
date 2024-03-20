package pl.edu.agh.firevox.shared.model

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository

@Service
class ChunkRepository(
    private val voxelRepository: VoxelRepository,
    private val materialRepository: PhysicalMaterialRepository,
    private val simulationRepository: SimulationsRepository,
) {

    fun saveAll(voxels: VoxelsChunk): MutableList<Voxel> = voxelRepository.saveAll(voxels.flatten())

    fun fetch(sKey: VoxelKey, eKey: VoxelKey) = VoxelsChunk(sKey, eKey, simulationRepository.fetchSize()).also {
        val air: PhysicalMaterial = materialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        it.voxels = Array(eKey.x - sKey.x) { x ->
            Array(eKey.y - sKey.y) { y ->
                Array(eKey.z - sKey.z) { z ->
                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = air,
                        evenIterationTemperature = air.baseTemperature,
                        oddIterationMaterial = air,
                        oddIterationTemperature = air.baseTemperature,
                        isBoundaryCondition = false
                    )
                }
            }
        }

        voxelRepository.findElementsByIndices(
            sKey.x, sKey.y, sKey.z, eKey.x, eKey.y, eKey.z,
        ).forEach {
            voxel -> it.voxels[voxel.key - sKey] = voxel
        }

    }
}

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
    else (k.x < endingVoxel.x - startingVoxel.x && k.y < endingVoxel.y - startingVoxel.y && k.z < endingVoxel.z - startingVoxel.z)

    fun flatten(): List<Voxel> {
        val flatten: List<Array<Voxel?>> = voxels.flatten()
        return flatten.flatMap { it.filterNotNull() }.toList()
    }
}

private inline operator fun Array<Array<Array<Voxel?>>>.get(key: VoxelKey): Voxel? = this[key.x][key.y][key.z]
private inline operator fun Array<Array<Array<Voxel?>>>.set(key: VoxelKey, value: Voxel) {
    this[key.x][key.y][key.z] = value
}