package pl.edu.agh.firevox.shared.model

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository

@Service
class ChunkRepository(
    private val voxelRepository: VoxelRepository,
    private val materialRepository: PhysicalMaterialRepository,
    private val simulationRepository: SimulationsRepository,
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun saveAll(voxels: VoxelsChunk): MutableList<Voxel> = voxelRepository.saveAll(voxels.flatten())

    fun fetch(sKey: VoxelKey, eKey: VoxelKey) = VoxelsChunk(sKey, eKey, simulationRepository.fetchSize()).also {
        val air: PhysicalMaterial = materialRepository.findByVoxelMaterial(VoxelMaterial.AIR)
        it.voxels = Array(eKey.x - sKey.x + 1) { x ->
            Array(eKey.y - sKey.y + 1) { y ->
                Array(eKey.z - sKey.z + 1) { z ->
                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = air,
                        evenIterationTemperature = air.baseTemperature,
                        oddIterationMaterial = air,
                        oddIterationTemperature = air.baseTemperature,
                        isBoundaryCondition = false
                    )
                }
            }
        }

        voxelRepository.findElementsByIndices(
            sKey.x, sKey.y, sKey.z, eKey.x, eKey.y, eKey.z,
        ).forEach { voxel ->
//            log.info("Inserting voxel ${voxel.key} in position ${voxel.key - sKey}")
            it.voxels[voxel.key - sKey] = voxel
        }

    }
}

private inline operator fun Array<Array<Array<Voxel?>>>.get(key: VoxelKey): Voxel? = this[key.x][key.y][key.z]
private inline operator fun Array<Array<Array<Voxel?>>>.set(key: VoxelKey, value: Voxel) {
    this[key.x][key.y][key.z] = value
}