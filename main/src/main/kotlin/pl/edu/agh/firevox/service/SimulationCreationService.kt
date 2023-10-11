package pl.edu.agh.firevox.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.PointsToNormals
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import java.util.*

@Service
class SimulationCreationService(
    private val modelMergeService: ModelMergeService,
    private val voxelRepository: CustomVoxelRepository,
    private val simulationRepository: SimulationsRepository,
    private val physicalMaterialRepository: PhysicalMaterialRepository,
    private val radiationPreprocessingStarter: RadiationPreprocessingStarter,
) {

    fun start(m: ModelDescriptionDto) = modelMergeService.createModel(m).let { s ->
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

        // process radiation here?? do we have the memory to create 3d array here or
        radiationPreprocessingStarter.start(m.pointsOfPlanesForRadiation ?: PointsToNormals(listOf()))

//      TODO -> move to the radiationPreprocessing finish, move canTransitionInFirstIteration() to database check
//        s.voxels.filter { VoxelMaterial.fromId(it.value).canTransitionInFirstIteration() }
//            .map { VoxelKeyIteration(it.key, 0) }
//            .forEach(messageSender::send)
    }
}

private fun VoxelMaterial.canTransitionInFirstIteration(): Boolean = setOf(
    WOOD_HEATED,
    WOOD_BURNING,
    PLASTIC_HEATED,
    PLASTIC_BURNING,
    TEXTILE_HEATED,
    TEXTILE_BURNING,
    METAL_HEATED,
    METAL_HOT,
    METAL_VERY_HOT,
    GLASS_HEATED,
    GLASS_HOT,
    GLASS_VERY_HOT,
    FLAME,
).contains(this)

private fun Pair<VoxelKey, PhysicalMaterial>.toEntity() = Voxel(
    key = this.first,
    evenIterationNumber = 0,
    evenIterationTemperature = this.second.baseTemperature,
    evenIterationMaterial = this.second,
    oddIterationNumber = 0,
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