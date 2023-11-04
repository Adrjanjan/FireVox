package pl.edu.agh.firevox.shared.synchroniser

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class SynchronisePlanes(
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {
    val volume: Double = voxelLength.pow(3)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun synchroniseRadiation(
        iteration: Long,
        planeConnection: PlaneConnectionDto
    ) {
//        log.info("Synchronisation from plane ${planeConnection.parentId} to ${planeConnection.childId}")
        if (iteration % 2 == 0L) { // even finished increment even
            updateOddTemperature(planeConnection.qNet, planeConnection.childId, planeConnection.childVoxelsCount)
            updateOddTemperature(-planeConnection.qNet, planeConnection.parentId, planeConnection.parentVoxelsCount)
        } else { // odd finished increment odd
            updateEvenTemperature(planeConnection.qNet, planeConnection.childId, planeConnection.childVoxelsCount)
            updateEvenTemperature(-planeConnection.qNet, planeConnection.parentId, planeConnection.parentVoxelsCount)
        }
    }

    fun updateOddTemperature(qNet: Double, planeId: Int, voxelsCount: Int) {
        val sql = """
            UPDATE voxels v
            SET odd_iteration_temperature = v.odd_iteration_temperature +  
                                            $qNet / ($volume * m.density * m.specific_heat_capacity * $voxelsCount)
            from materials m
            WHERE (v.x, v.y, v.z) IN
                  (SELECT voxel_key_x, voxel_key_y, voxel_key_z
                   FROM plane_voxels
                   WHERE plane_id = $planeId)
            and m.id = v.odd_iteration_material_id
            and v.is_boundary_condition = false; 
        """.trimIndent()
        jdbcTemplate.update(sql)
    }

    fun updateEvenTemperature(qNet: Double, planeId: Int, voxelsCount: Int) {
        val sql = """
            UPDATE voxels v
            SET even_iteration_temperature = v.even_iteration_temperature +
                                            $qNet / ($volume * m.density * m.specific_heat_capacity * $voxelsCount)
            from materials m
            WHERE (v.x, v.y, v.z) IN
                  (SELECT voxel_key_x, voxel_key_y, voxel_key_z
                   FROM plane_voxels
                   WHERE plane_id = $planeId)
            and m.id = v.odd_iteration_material_id
            and v.is_boundary_condition = false;
        """.trimIndent()
        jdbcTemplate.update(sql)
    }
}