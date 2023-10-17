package pl.edu.agh.firevox.shared.synchroniser

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import kotlin.math.pow

/**
 * Moved to shared so can be used in tests in worker
 */
@Service
class SynchroniserImpl    (
    private val countersRepository: CountersRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
) {
    private val volume: Double = voxelLength.pow(3)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    fun resetCounters(): Long {
        val iteration = countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION)?.count!!
        log.info("Running synchronisation $iteration")

        val processedVoxels = countersRepository.findByIdOrNull(CounterId.PROCESSED_VOXEL_COUNT)!!
        val shouldBeProcessedVoxels =
            countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT)!!
        if (processedVoxels.count != shouldBeProcessedVoxels.count) throw IterationNotFinishedException(iteration)

        val processedPlanes = countersRepository.findByIdOrNull(CounterId.PROCESSED_RADIATION_PLANES_COUNT)!!
        val shouldBeProcessedPlanes =
            countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)!!
        if (processedPlanes.count != shouldBeProcessedPlanes.count) throw IterationNotFinishedException(iteration)

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

    @Transactional
    fun synchroniseRadiationResults(iteration: Long) {
        radiationPlaneRepository.updateTemperatures(iteration, volume)
//        radiationPlaneRepository.findWithPositiveQNets().forEach { radiationPlane ->
//            val voxel = radiationPlane.voxels.first()
//            if (iteration % 2 == 0L) { // even
//                val material = voxel.evenIterationMaterial
//                radiationPlane.childPlanes.forEach { connection ->
//                    val voxelsCount = connection.child.voxels.count()
//                    val sourceMass = voxelsCount * volume * material.density
//                    val tempIncrease = connection.qNet / (sourceMass * material.specificHeatCapacity * voxelsCount)
//                    connection.child.voxels.forEach { it.evenIterationTemperature += tempIncrease }
//                }
//            } else { // odd
//                val material = voxel.oddIterationMaterial
//                radiationPlane.childPlanes.forEach { connection ->
//                    val voxelsCount = connection.child.voxels.count()
//                    val sourceMass = voxelsCount * volume * material.density
//                    val tempIncrease = connection.qNet / (sourceMass * material.specificHeatCapacity * voxelsCount)
//                    connection.child.voxels.forEach { it.oddIterationTemperature += tempIncrease }
//                }
//            }
//        }
    }

}

class IterationNotFinishedException(iteration: Long) : Exception("$iteration")