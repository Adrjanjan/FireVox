package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.StateProperties
import pl.edu.agh.firevox.shared.model.Voxel
import kotlin.math.pow

@Service
class ConductionCalculator(
    @Value("firevox.voxel.size") val voxelLength: Double,
) : PhysicsCalculator {

    override fun calculate(voxel: Voxel, voxels: List<Voxel>, timeStep: Double): StateProperties {
        val volume: Double = voxelLength.pow(3)
        val current = voxel.currentProperties
        val currentMaterial = current.material

        val lambda = currentMaterial.thermalConductivityCoefficient /
                (currentMaterial.density * volume * currentMaterial.specificHeatCapacity)

        val deltaT = timeStep * voxels.filter { includeInConduction(it, voxel) }.sumOf {
            lambda * (current.temperature - it.currentProperties.temperature)
        }
        return StateProperties(
            current.iterationNumber + 1,
            current.material,
            current.temperature + deltaT
        )
    }

    private fun includeInConduction(it: Voxel, voxel: Voxel) = it != voxel // also include gases?

}
