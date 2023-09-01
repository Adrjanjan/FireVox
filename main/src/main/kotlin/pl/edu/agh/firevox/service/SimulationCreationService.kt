package pl.edu.agh.firevox.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.messaging.MessageSender
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.Simulation
import pl.edu.agh.firevox.model.SingleModel
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.repository.SimulationsRepository
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*
import java.util.*

@Service
class SimulationCreationService(
    private val modelMergeService: ModelMergeService,
    private val voxelRepository: CustomVoxelRepository,
    private val simulationRepository: SimulationsRepository,
    private val messageSender: MessageSender,
) {

    fun start(m: ModelDescriptionDto) = modelMergeService.createModel(m).let { s ->
        val simulationId = UUID.randomUUID()
        simulationRepository.save(
            Simulation(
                id = simulationId, name = m.outputName, parentModel = m.parentModel.toEntity()
            )
        )
        s.voxels.map { it.toEntity() }.forEach(voxelRepository::save)
        s.voxels.filter { it.value.canTransitionInFirstIteration() }.map { VoxelKeyIteration(it.key, 0) }
            .forEach(messageSender::send)
    }
}

private fun VoxelMaterial.canTransitionInFirstIteration(): Boolean = setOf(
    HALF_SMOKE,
    FULL_SMOKE,
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

private fun Map.Entry<VoxelKey, VoxelMaterial>.toEntity() = Voxel(
    voxelKey = this.key, currentProperties = StateProperties(
        iterationNumber = 0, material = this.value
    ), nextProperties = StateProperties(
        iterationNumber = 0, material = this.value
    )
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