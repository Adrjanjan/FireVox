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
    @Value("\${firevox.timestep}") val timeStep: Double,
) {
    val volume: Double = voxelLength.pow(3)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun synchroniseRadiation(
        iteration: Int,
        planeConnection: PlaneConnectionDto
    ) {
//        log.info("Synchronisation from plane ${planeConnection.parentId} to ${planeConnection.childId}")
        // to secure when calculating for ambience that has 0 voxels
        val childVoxelsCount = if(planeConnection.childVoxelsCount == 0) 1 else planeConnection.childVoxelsCount
        val parentVoxelsCount = if(planeConnection.parentVoxelsCount == 0) 1 else planeConnection.parentVoxelsCount
        if (iteration % 2 == 0) { // even finished increment even
            updateOddTemperature(-planeConnection.qNet, planeConnection.childId, childVoxelsCount)
            updateOddTemperature(planeConnection.qNet, planeConnection.parentId, parentVoxelsCount)
        } else { // odd finished increment odd
            updateEvenTemperature(-planeConnection.qNet, planeConnection.childId, childVoxelsCount)
            updateEvenTemperature(planeConnection.qNet, planeConnection.parentId, parentVoxelsCount)
        }
    }

    fun updateOddTemperature(qNet: Double, planeId: Int, voxelsCount: Int) {
        val sql = """
            UPDATE voxels v
            SET odd_iteration_temperature = v.odd_iteration_temperature +  
                                            $timeStep * $qNet / ($volume * m.density * m.specific_heat_capacity * $voxelsCount)
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
                                            $timeStep * $qNet / ($volume * m.density * m.specific_heat_capacity * $voxelsCount)
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