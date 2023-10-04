package pl.edu.agh.firevox.shared.model

import jakarta.persistence.Tuple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.model.simulation.PaletteType
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser

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

    fun findAllForPalette(paletteName: PaletteType): Map<VoxelKey, Int> {
        return when (paletteName) {
            PaletteType.TEMPERATURE_PALETTE -> voxelRepository.findAllOnlyTemperatures()
                .map { (it.get("key") as VoxelKey )to (it.get("value__") as Double) }
                .let { result ->
                val min = result.minOf { it.second }
                val max = result.maxOf { it.second }

                result.associate {
                    it.first to VoxFormatParser.toPaletteLinear(
                        value = it.second,
                        min = min,
                        max = max
                    )
                }
            }
            PaletteType.BASE_PALETTE -> voxelRepository.findAllOnlyValues()
                .associate { (it.get("key") as VoxelKey )to (it.get("value__") as Int) }
        }
    }

}

interface VoxelRepository : JpaRepository<Voxel, VoxelKey> {
    fun findByKeyAndEvenIterationNumber(voxelKey: VoxelKey, iterationNumber: Int): Voxel?
    fun findByKeyAndOddIterationNumber(voxelKey: VoxelKey, iterationNumber: Int): Voxel?
    @Query(
        """
            select v.key as key, 
            case when v.evenIterationNumber > v.oddIterationNumber then v.evenIterationMaterial.id else v.oddIterationMaterial.id end as value__
            from Voxel v
        """
    )
    fun findAllOnlyValues(): List<Tuple>

    @Query(
        """
            select v.key as key, 
            case when v.evenIterationNumber > v.oddIterationNumber then v.evenIterationTemperature else v.oddIterationTemperature end as value__
            from Voxel v
        """
    )
    fun findAllOnlyTemperatures(): List<Tuple>

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