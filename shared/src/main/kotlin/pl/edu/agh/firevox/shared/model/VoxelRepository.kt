package pl.edu.agh.firevox.shared.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.config.FireVoxProperties.Companion.maxSize

@Repository
class CustomVoxelRepository(
    private val voxelRepository: VoxelRepository
) {
    fun findNeighbors(key: VoxelKey, type: NeighbourhoodType, iteration: Int) = type.keyMapping
        .map { key.copy(x = key.x + it.x, y = key.y + it.y, z = key.z + it.z) }
        .filter(::verifyInbound)
        .mapNotNull { findForIteration(key, iteration) }

    fun save(voxel: Voxel) = voxelRepository.save(voxel)

    private fun verifyInbound(k: VoxelKey) =
        if (k.x < 0 || k.y < 0 || k.z < 0) false
        else !(k.x > maxSize || k.y > maxSize || k.z > maxSize)

    fun findForIteration(key: VoxelKey, iteration: Int) =
        voxelRepository.findByVoxelKeyAndCurrentPropertiesIterationNumber(key, iteration)
}

interface VoxelRepository : JpaRepository<Voxel, VoxelKey> {
    fun findByVoxelKeyAndCurrentPropertiesIterationNumber(voxelKey: VoxelKey, iterationNumber: Int): Voxel?
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