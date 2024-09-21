package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.min

@Service
class SmokeCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
) {

    fun calculate(
        currentVoxel: VoxelState,
        neighbours: List<VoxelState>,
        timeStep: Double,
        iteration: Int,
        voxelsToSend: MutableSet<VoxelKey>,
    ): Double {
        val smokeTransferred = smokeTransferred(currentVoxel, neighbours)
        val smokeGenerated = smokeGenerated(currentVoxel, neighbours, timeStep)
        return currentVoxel.smokeConcentration + smokeTransferred + smokeGenerated
    }

    private fun smokeGenerated(currentVoxel: VoxelState, neighbours: List<VoxelState>, timeStep: Double): Double =
        neighbours.firstOrNull { it.isBelow(currentVoxel) && it.material.isBurning() }
            ?.let { generatedSmoke(it, timeStep) } ?: 0.0

    fun smokeTransferred(currentVoxel: VoxelState, neighbours: List<VoxelState>): Double {
        val transferOut = -smokeTransfer(currentVoxel, neighbours)
        val transferIn = neighbours.filter { it.material.transfersSmoke() }
            .sumOf { smokeTransfer(it, listOf(currentVoxel)) }
        return transferOut + transferIn
    }

    fun smokeTransfer(currentVoxel: VoxelState, neighbours: List<VoxelState>): Double {
        return neighbours.filter { it != currentVoxel && it.material.transfersSmoke() }.sumOf {
            individualTransfer(currentVoxel, it)
        }
    }

    fun individualTransfer(source: VoxelState, target: VoxelState): Double {
        val upperCanAccept = target.material.transfersSmoke() && target.smokeConcentration <= 0.99
        val transferFactor = if (upperCanAccept) {
            when {
                target.isAbove(source) -> 1.0
                target.isBelow(source) -> 0.25
                else -> 0.5
            }
        } else when {
            target.isAbove(source) -> 0.0
            target.isBelow(source) -> 0.5
            else -> 1.0
        }
        return transferFactor * min(source.smokeConcentration / 6.0, (1.0 - target.smokeConcentration) / 6.0)
    }


    private fun generatedSmoke(n: VoxelState, timeStep: Double): Double {
        return n.material.smokeEmissionPerSecond!! * timeStep / (voxelLength * voxelLength)
    }

}