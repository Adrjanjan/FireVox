package pl.edu.agh.firevox.worker.physics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    @Value("\${firevox.voxel.size}") private val voxelLength: Double = 0.01,
) {
    private val stefanBoltzmann = 5.67e-8 // W/(m^2 * K^4)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val delta = 10e-10

    //    @Transactional
    fun calculateWithVoxelsFilled(radiationPlane: RadiationPlane, iteration: Int): Boolean {
//        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false

//        val avgTemperature = avgForPlane(radiationPlane.id!!, iteration)
        val avgTemperature = radiationPlane.getTempAverage(iteration)
        radiationPlane.childPlanes.forEach {
//            val planeAverageTemperature = avgForPlane(it.child.id!!, iteration)
            val planeAverageTemperature = it.child.getTempAverage(iteration)
            if (avgTemperature > planeAverageTemperature) {
                val qNet = it.viewFactor *
                        stefanBoltzmann *
                        radiationPlane.area *
                        (avgTemperature.pow(4) - planeAverageTemperature.pow(4))
//                it.qNet = qNet // if(qNet > delta) qNet else 0.0
                jdbcTemplate.update("update planes_connections set q_net = $qNet where id = ${it.id}")
            }
        }
//        countersRepository.increment(CounterId.PROCESSED_RADIATION_PLANES_COUNT)
        return true
    }

//    @Transactional
//    fun calculateFetchingFromDb(radiationPlaneId: Int, iteration: Int): Boolean {
//        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false
////        log.info("Calculating for plane $radiationPlaneId and iteration $iteration")
//        val radiationPlane = radiationPlaneRepository.findByIdOrNull(radiationPlaneId)
//            ?: return true.also { log.error("Radiation plane with id $radiationPlaneId was not found") }
//
//        val avgTemperature = avgForPlane(radiationPlane.id!!, iteration)
//        radiationPlane.childPlanes.forEach {
//            val planeAverageTemperature = avgForPlane(it.child.id!!, iteration)
//            if (avgTemperature > planeAverageTemperature) { // hmm, może to powinno być bez tego warunku, wtedy mamy uwzględnione też wypromieniowanie?
//                val qNet = it.viewFactor *
//                        stefanBoltzmann *
//                        radiationPlane.area *
//                        (avgTemperature.pow(4) - planeAverageTemperature.pow(4))
//                it.qNet = qNet // if(qNet > delta) qNet else 0.0
//            }
//        }
//        countersRepository.increment(CounterId.PROCESSED_RADIATION_PLANES_COUNT)
//        return true
//    }
//
//    fun avgForPlane(planeId: Int, iteration: Int): Double = jdbcTemplate.queryForObject(
//        """
//            select case when MOD($iteration, 2) = 0
//                then avg(v.even_iteration_temperature)
//                else avg(v.odd_iteration_temperature) end as temp
//            from voxels v
//            where (v.x, v.y, v.z) IN (select p.voxel_key_x, p.voxel_key_y, p.voxel_key_z from plane_voxels p where p.plane_id = $planeId)
//        """
//    ) { r, _ -> r.getDouble("temp") }!!
//


    // parent ---promien--> child
    @Transactional
    fun calculateFetchingFromDb(wallId: Int, iteration: Int): Boolean {
        if (!countersRepository.canExecuteForIteration(iteration.toLong())) return false
        log.info("Calculating for wall $wallId and iteration $iteration")
        val planes = radiationPlaneRepository.findByWallId(wallId)
        planes.stream().parallel()
            .forEach { parent ->
                val parentAvgTemp = parent.getTempAverage(iteration)
                parent.childPlanes.forEach { child ->
                    val childAvgTemp = child.child.getTempAverage(iteration)
                    if (parentAvgTemp > childAvgTemp) {
                        val qNet = child.viewFactor *
                                stefanBoltzmann *
                                child.child.area *
                                (parentAvgTemp.pow(4) - childAvgTemp.pow(4))
                        child.qNet = qNet // if(qNet > delta) qNet else 0.0
                    }
                }
            }
        countersRepository.add(CounterId.PROCESSED_RADIATION_PLANES_COUNT, planes.count())
        return true
    }
}
//even_radiation_avarages
//odd_radiation_avarages