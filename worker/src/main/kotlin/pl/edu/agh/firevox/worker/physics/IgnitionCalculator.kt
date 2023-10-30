package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.PhysicalMaterial
import pl.edu.agh.firevox.shared.model.PhysicalMaterialRepository
import pl.edu.agh.firevox.shared.model.VoxelMaterial
import pl.edu.agh.firevox.worker.service.VoxelState

@Service
class IgnitionCalculator(
    private val physicalMaterialRepository: PhysicalMaterialRepository
) {

    private val materialToBurning = mapOf(
        VoxelMaterial.TEXTILE to VoxelMaterial.TEXTILE_BURNING,
        VoxelMaterial.WOOD to VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.PLASTIC to VoxelMaterial.PLASTIC_BURNING,
    )

    fun calculate(voxel: VoxelState, neighbours: List<VoxelState>, timestep: Double, iteration: Int): PhysicalMaterial {
        // ignition -> burning
        if (voxel.temperature >= voxel.material.autoignitionTemperature!!
            || iteration >= voxel.ignitingEndIteration!!
        ) {
            return physicalMaterialRepository.findByVoxelMaterial(materialToBurning[voxel.material.voxelMaterial]!!)
        } else {
            // TODO
            voxel.ignitingCounter += 1
        }
        // ignition increment

        return voxel.material
    }

}