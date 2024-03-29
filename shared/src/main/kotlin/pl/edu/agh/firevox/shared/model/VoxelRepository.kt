package pl.edu.agh.firevox.shared.model

import jakarta.persistence.Tuple
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.model.simulation.PaletteType
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.shared.verifyInbound

@Repository
class CustomVoxelRepository(
    private val voxelRepository: VoxelRepository,
    private val simulationRepository: SimulationsRepository,
) {
    fun findNeighbors(
        key: VoxelKey,
        type: NeighbourhoodType
    ): Pair<List<Voxel>, Set<VoxelKey>> {
        val result = type.keyMapping
            .map { key.copy(x = key.x + it.x, y = key.y + it.y, z = key.z + it.z) }
            .filter { verifyInbound(it, simulationRepository.fetchSize()) }
            .associateWith { findByIdOrNull(it) }
        val notNullFound = result.values.filterNotNull().toList()
        val keysToFill = result.filter { it.value == null }.keys
        return notNullFound to keysToFill
    }

    fun save(voxel: Voxel) = voxelRepository.save(voxel)

    fun saveAll(voxels: Collection<Voxel>) = voxelRepository.saveAll(voxels)

    fun findByIdOrNull(key: VoxelKey) = voxelRepository.findByIdOrNull(key)

    fun findAllForPalette(paletteName: PaletteType, iteration: Long): Map<VoxelKey, Int> {
        return when (paletteName) {
            PaletteType.TEMPERATURE_PALETTE -> voxelRepository.findAllOnlyTemperatures(iteration)
                .map { (it.get("key") as VoxelKey) to (it.get("value__") as Double) }
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

            PaletteType.BASE_PALETTE -> voxelRepository.findAllOnlyValues(iteration)
                .associate { (it.get("key") as VoxelKey) to (it.get("value__") as Int) }
        }
    }

    fun findStartingVoxels(firstIterationMaterials: Set<VoxelMaterial>) =
        voxelRepository.findStartingVoxels(firstIterationMaterials)

}

interface VoxelRepository : JpaRepository<Voxel, VoxelKey> {

    @Query(
        """
            select v.key as key, 
            case when MOD(:iteration, 2) = 0 then v.evenIterationMaterial.id else v.oddIterationMaterial.id end as value__
            from Voxel v
        """
    )
    fun findAllOnlyValues(iteration: Long): List<Tuple>

    @Modifying
    @Query("update Voxel v set v.evenIterationTemperature = v.evenIterationTemperature + :tempIncrease where v.key in :keys")
    fun incrementEvenTemperature(keys: MutableSet<VoxelKey>, tempIncrease: Double)

    @Modifying
    @Query("update Voxel v set v.oddIterationTemperature = v.oddIterationTemperature + :tempIncrease where v.key in :keys")
    fun incrementOddTemperature(keys: MutableSet<VoxelKey>, tempIncrease: Double)


    @Query(
        """
            select v.key as key, 
            case when MOD(:iteration, 2) = 0 then v.evenIterationTemperature else v.oddIterationTemperature end as value__
            from Voxel v
        """
    )
    fun findAllOnlyTemperatures(iteration: Long): List<Tuple>

    @Query(
        """
            select v.key as key from Voxel v where v.evenIterationMaterial in :firstIterationMaterials
        """
    )
    fun findStartingVoxels(firstIterationMaterials: Set<VoxelMaterial>): List<VoxelKey>


    @Query(
        value = "SELECT v FROM Voxel v " +
                "WHERE v.key.x >= :startX AND v.key.x <= :endX " +
                "AND v.key.y >= :startY AND v.key.y <= :endY " +
                "AND v.key.z >= :startZ AND v.key.z <= :endZ"
    )
    fun findElementsByIndices(
        @Param("startX") startX: Int,
        @Param("startY") startY: Int,
        @Param("startZ") startZ: Int,
        @Param("endX") endX: Int,
        @Param("endY") endY: Int,
        @Param("endZ") endZ: Int
    ): List<Voxel>
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