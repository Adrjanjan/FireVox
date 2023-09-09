package pl.edu.agh.firevox.shared.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.config.FireVoxProperties.Companion.maxSize
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView

@Repository
class CustomVoxelRepository(
    private val voxelRepository: VoxelRepository
) {
    fun findNeighbors(key: VoxelKey, type: NeighbourhoodType, iteration: Int, modelSize: SimulationSizeView): Pair<List<Voxel>, Set<VoxelKey>> {
        val result = type.keyMapping
            .map { key.copy(x = key.x + it.x, y = key.y + it.y, z = key.z + it.z) }
            .filter { verifyInbound(it, modelSize) }
            .associateWith { findForIteration(it, iteration) }
        return result.values.filterNotNull().toList() to result.filter { it.value == null }.keys
    }

    fun save(voxel: Voxel) = voxelRepository.save(voxel)

    private fun verifyInbound(k: VoxelKey, modelSize: SimulationSizeView) =
        if (k.x < 0 || k.y < 0 || k.z < 0) false
        else !(k.x > modelSize.sizeX || k.y > modelSize.sizeY || k.z > modelSize.sizeZ)

    fun findForIteration(key: VoxelKey, iteration: Int) = when(iteration % 2) {
        0 -> voxelRepository.findByKeyAndEvenIterationNumber(key, iteration - 1)
        1 -> voxelRepository.findByKeyAndOddIterationNumber(key, iteration - 1)
        else -> null
    }
}

interface VoxelRepository : JpaRepository<Voxel, VoxelKey> {
    fun findByKeyAndEvenIterationNumber(voxelKey: VoxelKey, iterationNumber: Int): Voxel?
    fun findByKeyAndOddIterationNumber(voxelKey: VoxelKey, iterationNumber: Int): Voxel?
}

enum class NeighbourhoodType(val keyMapping: List<Triple<Int, Int, Int>>) {
    TOP(listOf(Triple(0, 0, 1))),
    BOTTOM(listOf(Triple(0, 0, -1))),
    N_E_W_S_U_L_(
        listOf(
            Triple(0, 0, 1),
            Triple(0, 0, -1),
            Triple(0, 1, 0),
            Triple(0, -1, 0),
            Triple(1, 0, 0),
            Triple(-1, 0, 0),
        )
    ),
    N_E_W_S_U_(
        listOf(
            Triple(0, 0, 1),
            Triple(0, 1, 0),
            Triple(0, -1, 0),
            Triple(1, 0, 0),
            Triple(-1, 0, 0),
        )
    ),
    N_E_W_S_(
        listOf(
            Triple(0, 1, 0),
            Triple(0, -1, 0),
            Triple(1, 0, 0),
            Triple(-1, 0, 0),
        )
    ),
    ALL(
        mutableListOf<Triple<Int, Int, Int>>().let {
            for (i in -1..1)
                for (j in -1..1)
                    for (k in -1..1)
                        it.add(Triple(i, j, k))
            it
        }
    )
}

val Triple<Int, Int, Int>.x: Int
    get() = this.first
val Triple<Int, Int, Int>.y: Int
    get() = this.second
val Triple<Int, Int, Int>.z: Int
    get() = this.third