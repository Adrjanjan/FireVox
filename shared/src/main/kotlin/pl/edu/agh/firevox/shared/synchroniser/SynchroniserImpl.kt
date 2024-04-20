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
class SynchroniserImpl    (
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

        synchroniseRadiationResults(iteration)

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

    fun synchroniseRadiationResults(iteration: Long) {
        val planesConnections = getPlanesConnections()
        if(iteration % 2 == 0L) {
            jdbcTemplate.update("refresh materialized view even_radiation_averages;")
        } else {
            jdbcTemplate.update("refresh materialized view odd_radiation_averages;")
        }

        planesConnections.parallelStream().forEach { connection ->
            synchronisePlanes.synchroniseRadiation(iteration, connection)
        }
        jdbcTemplate.update("update planes_connections set q_net = 0.0")
    }

    fun getPlanesConnections(): List<PlaneConnectionDto> {
        val sql = "SELECT pc.id, pc.q_net, pc.child_plane_id, pc.parent_plane_id, pc.parent_voxels_count, pc.child_voxels_count " +
                "FROM planes_connections pc " +
                "WHERE pc.q_net > 0.0"
        return jdbcTemplate.query(sql, planeConnectionDtoRowMapper)
    }

    private val planeConnectionDtoRowMapper = RowMapper<PlaneConnectionDto>{ rs: ResultSet, _: Int ->
        PlaneConnectionDto(
            rs.getInt("parent_id"),
            rs.getInt("child_id"),
            rs.getDouble("q_net"),
            rs.getInt("parent_voxels_count"),
            rs.getInt("child_voxels_count"),
        )
    }

}

data class PlaneConnectionDto(
    val parentId: Int,
    val childId: Int,
    val qNet: Double,
    val parentVoxelsCount: Int,
    val childVoxelsCount: Int,
)

class IterationNotFinishedException(iteration: Long) : Exception("$iteration")