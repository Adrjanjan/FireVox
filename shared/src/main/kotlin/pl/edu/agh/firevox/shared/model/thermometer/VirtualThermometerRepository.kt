package pl.edu.agh.firevox.shared.model.thermometer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.VoxelKey

interface VirtualThermometerRepository : JpaRepository<VirtualThermometer, Int> {

    @Transactional
    @Modifying
    @Query("UPDATE VirtualThermometer t SET t.measurements = CONCAT(t.measurements, CONCAT(', ', :v)) WHERE t.voxelKey = :key")
    fun updateValue(key: VoxelKey, v: Double)

    fun findByVoxelKey(key: VoxelKey): VirtualThermometer?

    fun existsByVoxelKey(voxelKey: VoxelKey): Boolean

    @Transactional
    @Modifying
    @Query("delete from VirtualThermometer v where v.voxelKey = ?1")
    fun deleteByVoxelKey(voxelKey: VoxelKey): Int
}
