package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import kotlin.math.pow

@Service
class RadiationCalculator(
    private val voxelRepository: CustomVoxelRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    @Value("\${firevox.voxel.size}") val voxelLength: Double = 0.01,
) {
    private val stefanBoltzmann = 5.67e-8 // W/(m^2 * K^4)
    private val volume: Double = voxelLength.pow(3)

    @Transactional
    fun calculate(radiationPlaneId: Int, iteration: Int) {
        val radiationPlane = radiationPlaneRepository.findByIdOrNull(radiationPlaneId) ?: return
        val voxel = radiationPlane.voxels.first()
        val material = if (voxel.evenIterationNumber == iteration) voxel.evenIterationMaterial else voxel.oddIterationMaterial
        val avgTemperature = planeAverageTemperature(radiationPlane, iteration)
        radiationPlane.childPlanes.forEach {
            val planeAverageTemperature = planeAverageTemperature(it.child, iteration)
            if(avgTemperature > planeAverageTemperature) {
                val qNet = it.viewFactor *
                        stefanBoltzmann *
                        radiationPlane.area *
                        (avgTemperature.pow(4) - planeAverageTemperature)
                it.qNet = qNet
            }
        }
    }

    @Transactional
    fun synchroniseRadiationResults(iteration: Int) {
        val radiationPlanes = radiationPlaneRepository.findAll().forEach { radiationPlane ->
            val voxel = radiationPlane.voxels.first()
            if (iteration % 2 == 0) { // even
                val material = voxel.evenIterationMaterial
                val sourceMass = radiationPlane.voxels.size * volume * material.density
                radiationPlane.childPlanes.forEach {
                    val tempIncrease = it.qNet / (sourceMass * material.specificHeatCapacity)
                    TODO()
                }
            } else { // odd

            }

        }
    }
//    TODO przekminić do końca synchronizację na dłuższą metę i może te countery dodać


    // TODO(change to repository function?)
    private fun planeAverageTemperature(
        radiationPlane: RadiationPlane,
        iteration: Int
    ) = radiationPlane.voxels.sumOf {
        if (it.evenIterationNumber == iteration) it.evenIterationTemperature else it.oddIterationTemperature
    } / radiationPlane.voxels.size


}