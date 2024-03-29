package pl.edu.agh.firevox.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.messaging.VoxelProcessingMessageSender
import pl.edu.agh.firevox.shared.messaging.RadiationPlanesProcessingMessageSender
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.PointsToNormals
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneDto
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.shared.model.simulation.counters.Counter
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.synchroniser.SynchronisationJobManager
import java.util.*
import java.util.function.Consumer

@Service
class SimulationCreationService(
    private val modelMergeService: ModelMergeService,
    private val voxelRepository: CustomVoxelRepository,
    private val simulationRepository: SimulationsRepository,
    private val physicalMaterialRepository: PhysicalMaterialRepository,
    private val radiationPreprocessingStarter: RadiationPreprocessingStarter,
    private val synchronisationJobManager: SynchronisationJobManager,
    private val voxelProcessingMessageSender: VoxelProcessingMessageSender,
    private val radiationPlanesProcessingMessageSender: RadiationPlanesProcessingMessageSender,
    private val countersRepository: CountersRepository,
    private val radiationPlaneRepository: RadiationPlaneRepository,
    @Value("\${firevox.timestep}") private val timeStep: Double,
    @Value("\${firevox.radiation.planes.minimalAvgTemperatureForStart}") private val minimalAvgTemperature: Double,
) {

    @Transactional
    fun preprocess(m: ModelDescriptionDto) = modelMergeService.createModel(m).let { s ->
        val simulationId = UUID.randomUUID()
        val materials = physicalMaterialRepository.findAll().associateBy { it.voxelMaterial }
        simulationRepository.save(
            Simulation(
                id = simulationId,
                name = m.outputName,
                parentModel = m.parentModel.toEntity(),
                sizeX = s.sizeX,
                sizeY = s.sizeY,
                sizeZ = s.sizeZ,
            )
        )
        s.voxels.map { it.key to materials[VoxelMaterial.fromId(it.value)]!! }
            .map { it.toEntity() }.forEach(voxelRepository::save)

        radiationPreprocessingStarter.start(m.pointsOfPlanesForRadiation ?: PointsToNormals(listOf()))

        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (m.simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
    }

    @Bean
    fun simulationStart(): Consumer<Boolean> = Consumer<Boolean> { _ ->
        voxelRepository.findStartingVoxels(firstIterationMaterials)
            .map { VoxelKeyIteration(it, 0) }
            .also { countersRepository.set(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, it.size.toLong()) }
            .forEach(voxelProcessingMessageSender::send)

        radiationPlaneRepository.findStartingPlanes(minimalAvgTemperature)
            .map {  RadiationPlaneDto(it, 0) }
            .also { countersRepository.set(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, it.size.toLong()) }
            .forEach(radiationPlanesProcessingMessageSender::send)

        synchronisationJobManager.scheduleJob()
    }

    private val firstIterationMaterials = setOf(
        WOOD_BURNING,
        PLASTIC_BURNING,
        TEXTILE_BURNING,
        FLAME,
    )

}

private fun Pair<VoxelKey, PhysicalMaterial>.toEntity() = Voxel(
    key = this.first,
    evenIterationTemperature = this.second.baseTemperature,
    evenIterationMaterial = this.second,
    oddIterationMaterial = this.second,
    oddIterationTemperature = this.second.baseTemperature,
)

private fun SingleModelDto.toEntity(parentId: UUID? = null): SingleModel {
    val modelId = UUID.randomUUID()
    return SingleModel(
        id = modelId,
        name = this.name,
        scale = this.scale,
        positionX = this.positionX,
        positionY = this.positionY,
        positionZ = this.positionZ,
        centerX = this.centerX,
        centerY = this.centerY,
        centerZ = this.centerZ,
        flipX = this.flipX,
        flipY = this.flipY,
        flipZ = this.flipZ,
        rotateX = this.rotateX,
        rotateY = this.rotateY,
        rotateZ = this.rotateZ,
        childModels = this.childModels.map { it.toEntity(modelId) }.toList(),
        parentId = parentId,
    )
}