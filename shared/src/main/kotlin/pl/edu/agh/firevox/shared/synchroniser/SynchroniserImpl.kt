package pl.edu.agh.firevox.shared.synchroniser

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import kotlin.math.pow
import org.springframework.transaction.annotation.Transactional

/**
 * Moved to shared so can be used in tests in worker
 */
@Service
class SynchroniserImpl    (
    private val countersRepository: CountersRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    private val synchronisePlanes: SynchronisePlanes,
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
) {
    private val volume: Double = voxelLength.pow(3)

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
        if (processedVoxels.count != shouldBeProcessedVoxels.count) throw IterationNotFinishedException(iteration)

        val processedPlanes = countersRepository.findByIdOrNull(CounterId.PROCESSED_RADIATION_PLANES_COUNT)!!
        val shouldBeProcessedPlanes =
            countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)!!
        if (processedPlanes.count != shouldBeProcessedPlanes.count) throw IterationNotFinishedException(iteration)
        return iteration
    }

    fun synchroniseRadiationResults(iteration: Long) {
        radiationPlaneRepository.findWithPositiveQNets().parallelStream().forEach { radiationPlane ->
            log.info("Synchronisation for plane ${radiationPlane.id}")
            synchronisePlanes.synchroniseRadiation(iteration, radiationPlane)
        }
        radiationPlaneRepository.resetQNet()
    }


}

class IterationNotFinishedException(iteration: Long) : Exception("$iteration")