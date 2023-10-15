package pl.edu.agh.firevox.synchroniser

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import kotlin.math.pow

@Service
class IterationSynchroniser(
    private val countersRepository: CountersRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
    ) : Job {
    private val volume: Double = voxelLength.pow(3)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private var schedulingEnabled = false

    fun enableScheduling() {
        schedulingEnabled = true
    }

    fun disableScheduling() {
        schedulingEnabled = false
    }

    @Transactional
    override fun execute(jobExecutionContext: JobExecutionContext) {
        if (schedulingEnabled) {
            val iteration = countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION)?.count!!
            log.info("Running synchronisation $iteration")

            val processedVoxels = countersRepository.findByIdOrNull(CounterId.PROCESSED_VOXEL_COUNT)!!
            val shouldBeProcessedVoxels = countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT)!!
            if(processedVoxels.count != shouldBeProcessedVoxels.count) throw IterationNotFinishedException(iteration)

            val processedPlanes = countersRepository.findByIdOrNull(CounterId.PROCESSED_RADIATION_PLANES_COUNT)!!
            val shouldBeProcessedPlanes = countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)!!
            if(processedPlanes.count != shouldBeProcessedPlanes.count) throw IterationNotFinishedException(iteration)

            this.synchroniseRadiationResults(iteration)

            countersRepository.reset(CounterId.PROCESSED_RADIATION_PLANES_COUNT)
            countersRepository.set(
                CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
                countersRepository.findByIdOrNull(CounterId.NEXT_ITERATION_RADIATION_PLANES_COUNT)?.count ?: 0
            )
            countersRepository.reset(CounterId.NEXT_ITERATION_VOXEL_COUNT)

            countersRepository.reset(CounterId.PROCESSED_VOXEL_COUNT)
            countersRepository.set(
                CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT,
                countersRepository.findByIdOrNull(CounterId.NEXT_ITERATION_VOXEL_COUNT)?.count ?: 0
            )
            countersRepository.reset(CounterId.NEXT_ITERATION_VOXEL_COUNT)
            countersRepository.increment(CounterId.CURRENT_ITERATION)
        }
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

interface CountersRepository : JpaRepository<Counter, CounterId>, JpaSpecificationExecutor<RadiationPlane> {

    @Query(
        """
           update Counter c set c.count = c.count+1 where c.id = (:id) 
        """
    )
    @Modifying
    fun increment(@Param("id") id: CounterId)

    @Query(
        """
            update Counter c set c.count = (:value) where c.id = (:id) 
        """
    )
    @Modifying
    fun set(id: CounterId, value: Long)

    fun reset(id: CounterId) = set(id, 0)

}

@Entity
@Table(name = "counters")
class Counter(
    @Id
    val id: CounterId,
    val count: Long,
)

enum class CounterId {
    CURRENT_ITERATION,
    PROCESSED_VOXEL_COUNT,
    CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT,
    NEXT_ITERATION_VOXEL_COUNT,
    PROCESSED_RADIATION_PLANES_COUNT,
    CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
    NEXT_ITERATION_RADIATION_PLANES_COUNT,
}
