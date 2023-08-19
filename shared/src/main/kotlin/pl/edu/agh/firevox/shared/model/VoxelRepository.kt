package pl.edu.agh.firevox.shared.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.config.FireVoxProperties.Companion.maxSize

@Repository
class CustomVoxelRepository(
    private val voxelRepository: VoxelRepository
) {
    fun findWithNeighbors(key: VoxelKey, type: NeighbourhoodType, iteration: Int) = type.keyMapping
        .map { key.copy(x = key.x + it.first, y = key.y + it.second, z = key.z + it.third) }
        .filter(::verifyInbound)
        .associateWith{ voxelRepository.findByVoxelKeyAndCurrentIteration(it, iteration) }

    fun save(voxel: Voxel) = voxelRepository.save(voxel)

    private fun verifyInbound(k: VoxelKey) =
        if (k.x < 0 || k.y < 0 || k.z < 0) false
        else !(k.x > maxSize || k.y > maxSize || k.z > maxSize)

}

interface VoxelRepository : JpaRepository<Voxel,VoxelKey> {
    fun findByVoxelKeyAndCurrentIteration(voxelKey: VoxelKey, iteration: Int) : Voxel?
}

enum class NeighbourhoodType(val keyMapping: List<Triple<Int, Int, Int>>) {
    TOP(listOf(Triple(0, 0, 1))),
    BOTTOM(listOf(Triple(0, 0, -1))),
    SIX_SIDES(
        listOf(
            Triple(0, 0, 0),
            Triple(0, 0, 1),
            Triple(0, 0, -1),
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
