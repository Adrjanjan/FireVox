package pl.edu.agh.firevox.shared.model.thermometer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.VoxelKey

interface VirtualThermometerRepository : JpaRepository<VirtualThermometer, Int> {

    @Query("""
        select max(v.iteration) from VirtualThermometer v where v.voxelKey = ?1
    """)
    fun findLastIteration(key: VoxelKey): Int?

    @Query(
        """
        select v.measurement from VirtualThermometer v where v.voxelKey = ?1
    """
    )
    fun findMeasurements(key: VoxelKey): List<Double>

    @Transactional
    @Modifying
    @Query("delete from VirtualThermometer v where v.voxelKey = ?1")
    fun deleteByVoxelKey(voxelKey: VoxelKey): Int
}
