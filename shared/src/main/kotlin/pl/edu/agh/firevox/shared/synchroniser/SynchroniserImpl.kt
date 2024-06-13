package pl.edu.agh.firevox.shared.synchroniser

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import java.sql.ResultSet

/**
 * Moved to shared so can be used in tests in worker
 */
@Service
class SynchroniserImpl(
    private val countersRepository: CountersRepository,
    private val synchronisePlanes: SynchronisePlanes,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    private val jdbcTemplate: JdbcTemplate
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    fun resetCounters(iteration: Long): Long {
        verifyIterationFinish(iteration)

        synchroniseRadiationResults(iteration.toInt())

        countersRepository.reset(CounterId.PROCESSED_RADIATION_PLANES_COUNT)
        countersRepository.set(
            CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
            countersRepository.findByIdOrNull(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)?.count ?: 0
        )
        countersRepository.reset(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT)

        countersRepository.reset(CounterId.PROCESSED_VOXEL_COUNT)
        countersRepository.set(
            CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT,
            countersRepository.findByIdOrNull(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT)?.count ?: 0
        )
        countersRepository.reset(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT)
        countersRepository.increment(CounterId.CURRENT_ITERATION)
        return iteration
    }

    fun verifyIterationFinish(iteration: Long): Long {
        val processedVoxels = countersRepository.findByIdOrNull(CounterId.PROCESSED_VOXEL_COUNT)!!
        val shouldBeProcessedVoxels =
            countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT)!!
        // new voxels to process may be added dynamically
        if (processedVoxels.count >= shouldBeProcessedVoxels.count) throw IterationNotFinishedException(iteration)

        val processedPlanes = countersRepository.findByIdOrNull(CounterId.PROCESSED_RADIATION_PLANES_COUNT)!!
        val shouldBeProcessedPlanes =
            countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)!!
        if (processedPlanes.count >= shouldBeProcessedPlanes.count) throw IterationNotFinishedException(iteration)
        return iteration
    }

    fun simulationStartSynchronise() {
        jdbcTemplate.update("refresh materialized view odd_radiation_averages;")
        jdbcTemplate.update("refresh materialized view even_radiation_averages;")
    }

    fun synchroniseRadiationResults(iteration: Int) {
        val planesConnections = getPlanesConnections()
        planesConnections.parallelStream().forEach { connection ->
            synchronisePlanes.synchroniseRadiation(iteration, connection)
        }
        if (iteration % 2 == 0) {
            jdbcTemplate.update("refresh materialized view odd_radiation_averages;")
        } else {
            jdbcTemplate.update("refresh materialized view even_radiation_averages;")
        }
        jdbcTemplate.update("update planes_connections set q_net = 0.0")
    }

    fun getPlanesConnections(): List<PlaneConnectionDto> {
        val sql = """
            SELECT pc.id, pc.$qNet, pc.$childPlaneId, pc.$parentPlaneId, pc.$parentVoxelCount, pc.$childVoxelCount
                FROM planes_connections pc
                WHERE pc.q_net != 0.0
        """.trimIndent() // we use only less than 0 to include ambience in the calculations
        return jdbcTemplate.query(sql, planeConnectionDtoRowMapper)
    }

    private val planeConnectionDtoRowMapper = RowMapper<PlaneConnectionDto> { rs: ResultSet, _: Int ->
        PlaneConnectionDto(
            parentId = rs.getInt(parentPlaneId),
            childId = rs.getInt(childPlaneId),
            qNet = rs.getDouble(qNet),
            parentVoxelsCount = rs.getInt(parentVoxelCount),
            childVoxelsCount = rs.getInt(childVoxelCount),
        )
    }

    val qNet: String = "q_net"
    val parentPlaneId: String = "parent_plane_id"
    val childPlaneId: String = "child_plane_id"

    val parentVoxelCount: String = "parent_voxels_count"
    val childVoxelCount: String = "child_voxels_count"
}

data class PlaneConnectionDto(
    val parentId: Int,
    val childId: Int,
    val qNet: Double,
    val parentVoxelsCount: Int,
    val childVoxelsCount: Int,
)

class IterationNotFinishedException(iteration: Long) : Exception("$iteration")