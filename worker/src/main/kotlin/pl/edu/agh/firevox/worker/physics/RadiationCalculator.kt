package pl.edu.agh.firevox.worker.physics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.radiation.PlanesConnection
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import kotlin.math.pow

@Service
class RadiationCalculator(
    private val radiationPlaneRepository: RadiationPlaneRepository,
    private val countersRepository: CountersRepository,
    private val jdbcTemplate: JdbcTemplate,
    @Value("\${firevox.voxel.size}") private val voxelLength: Double = 0.001,
    @Value("\${firevox.voxel.ambient}") private val ambientTemp: Double = 293.15, // 20 Celsius
) {
    private val stefanBoltzmann = 5.67e-8 // W/(m^2 * K^4)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val delta = 10e-10

    //    @Transactional
    fun calculateWithVoxelsFilled(parent: RadiationPlane, iteration: Int): Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false

        val parentAvgTemp = parent.getTempAverage(iteration)
        parent.childPlanes.forEach { toChild ->
            if (toChild.isAmbient) {
                radiationForAmbience(toChild, parent, parentAvgTemp)
            } else {
                radiationForExistingChild(toChild, iteration, parentAvgTemp, parent)
            }
        }
        countersRepository.increment(CounterId.PROCESSED_RADIATION_PLANES_COUNT)
        return true
    }

    // parent ---promien--> child
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun calculateFetchingFromDb(wallId: Int, iteration: Int): Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false
        log.info("Calculating for wall $wallId and iteration $iteration")
        val planes = radiationPlaneRepository.findByWallId(wallId)
        planes.forEach { parent ->
            val parentAvgTemp = parent.getTempAverage(iteration)
            parent.childPlanes.forEach { toChild ->
                if (toChild.isAmbient) {
                    radiationForAmbience(toChild, parent, parentAvgTemp)
                } else {
                    radiationForExistingChild(toChild, iteration, parentAvgTemp, parent)
                }
            }
        }
        countersRepository.add(CounterId.PROCESSED_RADIATION_PLANES_COUNT, planes.count())
        return true
    }

    private fun radiationForAmbience(
        toChild: PlanesConnection,
        parent: RadiationPlane,
        parentAvgTemp: Double
    ) {
        val qNet = parent.lostRadiationPercentage *
                stefanBoltzmann *
                parent.area *
                (ambientTemp.pow(4) - parentAvgTemp.pow(4))
        toChild.qNet = qNet
    }

    private fun radiationForExistingChild(
        toChild: PlanesConnection,
        iteration: Int,
        parentAvgTemp: Double,
        parent: RadiationPlane
    ) {
        val childAvgTemp = toChild.child!!.getTempAverage(iteration)
        if (parentAvgTemp > childAvgTemp) {
            val qNet = toChild.viewFactor *
                    stefanBoltzmann *
                    parent.area *
                    (childAvgTemp.pow(4) - parentAvgTemp.pow(4))
            toChild.qNet = qNet
        }
    }
}