package pl.edu.agh.firevox.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.model.PointsToNormals
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.radiation.PlaneFinder
import pl.edu.agh.firevox.shared.model.simulation.PaletteType
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import java.util.function.Consumer

@Service
class RadiationPreprocessingStarter(
    private val streamBridge: StreamBridge,
    private val planeFinder: PlaneFinder,
    private val voxelRepository: CustomVoxelRepository,
    private val simulationsRepository: SimulationsRepository
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val topicName = "radiation-start"

    fun start(pointsToNormals: PointsToNormals) {
        log.info("Sending to start radiation preprocessing: $topicName, points size: ${pointsToNormals.points.size}")
        streamBridge.send(topicName, pointsToNormals)
    }

    @Bean
    fun radiationInput(): Consumer<PointsToNormals> {
        return Consumer<PointsToNormals> { pointsToNormals ->
            log.info("Starting radiation preprocessing")
            val voxels = voxelRepository.findAllForPalette(PaletteType.BASE_PALETTE)
            val size = simulationsRepository.fetchSize()

            val matrix = Array(size.sizeX - 1) { _ ->
                Array(size.sizeY - 1) { _ ->
                    IntArray(size.sizeZ - 1) { _ -> 0 }
                }
            }
            voxels.forEach { (t, u) ->
                matrix[t.x][t.y][t.z] = u
            }

            planeFinder.findPlanes(matrix, pointsToNormals.points)
        }
    }

}

