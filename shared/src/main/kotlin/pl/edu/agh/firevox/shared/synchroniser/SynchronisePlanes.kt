package pl.edu.agh.firevox.shared.synchroniser

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.VoxelRepository
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane

@Service
class SynchronisePlanes(
    private val voxelRepository: VoxelRepository,
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    fun synchroniseRadiation(
        iteration: Long,
        radiationPlane: RadiationPlane
    ) {
        if (iteration % 2 == 1L) { // odd finished increment even
            radiationPlane.childPlanes.forEach { connection ->
                    log.info("Synchronisation for plane ${radiationPlane.id} for connection ${connection.id}")
                voxelRepository.incrementEvenTemperature(connection.child.voxels, connection.qNet / connection.child.heatToTemperatureFactor)
                voxelRepository.incrementEvenTemperature(connection.parent.voxels, connection.qNet / connection.parent.heatToTemperatureFactor)
            }
        } else { // even finished increment odd
            radiationPlane.childPlanes.forEach { connection ->
                    log.info("Synchronisation for plane ${radiationPlane.id} for connection ${connection.id}")
                voxelRepository.incrementOddTemperature(connection.child.voxels, connection.qNet / connection.child.heatToTemperatureFactor)
                voxelRepository.incrementOddTemperature(connection.parent.voxels, connection.qNet / connection.parent.heatToTemperatureFactor)
            }
        }
    }
}