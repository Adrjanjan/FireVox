package pl.edu.agh.firevox.worker.physics

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.PhysicalMaterial
import pl.edu.agh.firevox.shared.model.PhysicalMaterialRepository
import pl.edu.agh.firevox.shared.model.VoxelMaterial
import pl.edu.agh.firevox.shared.model.VoxelState

@Service
class IgnitionCalculator(
    private val physicalMaterialRepository: PhysicalMaterialRepository
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val materialToBurning = mapOf(
        VoxelMaterial.TEXTILE to VoxelMaterial.TEXTILE_BURNING,
        VoxelMaterial.WOOD to VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.PLASTIC to VoxelMaterial.PLASTIC_BURNING,
    )

    fun calculate(voxel: VoxelState, neighbours: List<VoxelState>, timestep: Double, iteration: Int): PhysicalMaterial {
        return if (voxel.temperature >= voxel.material.autoignitionTemperature!!
            || voxel.material.timeToIgnition!! <= voxel.ignitingCounter * timestep
        ) { // ignition -> burning
            if(neighbours.any { it.material.canContainOxygen() }) {
                voxel.burningCounter = 0
                physicalMaterialRepository.findByVoxelMaterial(materialToBurning[voxel.material.voxelMaterial]!!)
            } else {
                log.info("Voxel ${voxel.key} can start burning, but no oxygen $iteration")
                voxel.material
            }
        } else {
            if(neighbours.any { it.temperature > voxel.material.ignitionTemperature!! }) // igniting
                voxel.ignitingCounter += 1
            voxel.material
        }
    }

}