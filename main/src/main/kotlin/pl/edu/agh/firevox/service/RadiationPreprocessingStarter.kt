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
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.PaletteType
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import java.util.function.Consumer

@Service
class RadiationPreprocessingStarter(
    private val streamBridge: StreamBridge,
    private val planeFinder: PlaneFinder,
    private val voxelRepository: CustomVoxelRepository,
    private val simulationsRepository: SimulationsRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    private val countersRepository: CountersRepository,
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
            val voxels = voxelRepository.findAllForPalette(PaletteType.BASE_PALETTE, 0)
            val size = simulationsRepository.fetchSize()

            val matrix = Array(size.sizeX) { _ ->
                Array(size.sizeY) { _ ->
                    IntArray(size.sizeZ) { _ -> 0 }
                }
            }
            voxels.forEach { (t, u) ->
                matrix[t.x][t.y][t.z] = u
            }

            val fakeRadiationPlane = RadiationPlane(
                id = 1,
                a = VoxelKey(0, 0, 0),
                b = VoxelKey(0, 0, 0),
                c = VoxelKey(0, 0, 0),
                d = VoxelKey(0, 0, 0),
                normalVector = VoxelKey(-1, 0, 0),
                voxels = mutableSetOf(),
                voxelsCount = 0,
                area = 0.0,
            ).let(radiationPlaneRepository::save)

            planeFinder.findPlanes(matrix, pointsToNormals.points, fakeRadiationPlane)
                .also {
                    countersRepository.set(
                        CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
                        it.size.toLong()
                    )
                }.also(radiationPlaneRepository::saveAll)
            streamBridge.send("simulation-start", true)
        }
    }

}

