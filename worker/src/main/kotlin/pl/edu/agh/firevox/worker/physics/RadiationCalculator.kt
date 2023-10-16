package pl.edu.agh.firevox.worker.physics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import kotlin.math.pow

@Service
class RadiationCalculator(
    private val radiationPlaneRepository: RadiationPlaneRepository,
    private val countersRepository: CountersRepository,
) {
    private val stefanBoltzmann = 5.67e-8 // W/(m^2 * K^4)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    fun calculate(radiationPlaneId: Int, iteration: Int): Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false

        val radiationPlane = radiationPlaneRepository.findByIdOrNull(radiationPlaneId) ?:
            return true.also { log.error("Radiation plane with id $radiationPlaneId was not found") }

        val avgTemperature = planeAverageTemperature(radiationPlane, iteration)
        radiationPlane.childPlanes.forEach {
            val planeAverageTemperature = planeAverageTemperature(it.child, iteration)
            if(avgTemperature > planeAverageTemperature) {
                val qNet = it.viewFactor *
                        stefanBoltzmann *
                        radiationPlane.area *
                        (avgTemperature.pow(4) - planeAverageTemperature.pow(4))
                it.qNet = qNet
            }
        }
        countersRepository.increment(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)
        countersRepository.increment(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT)
        return true
    }

    private fun planeAverageTemperature(
        radiationPlane: RadiationPlane,
        iteration: Int
    ) = radiationPlane.voxels.sumOf {
        if (it.evenIterationNumber == iteration) it.evenIterationTemperature else it.oddIterationTemperature
    } / radiationPlane.voxels.size

}