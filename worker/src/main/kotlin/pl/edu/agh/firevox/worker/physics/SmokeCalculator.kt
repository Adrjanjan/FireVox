package pl.edu.agh.firevox.worker.physics

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState
import kotlin.math.min
import kotlin.math.sign

@Service
class SmokeCalculator {

    fun calculate(
        currentVoxel: VoxelState,
        neighbours: List<VoxelState>,
        timeStep: Double,
        iteration: Int,
        voxelsToSend: MutableSet<VoxelKey>,
    ): Double {
        val smokeTransferred = smokeTransferred(currentVoxel, neighbours, timeStep)
        val smokeGenerated = smokeGenerated(currentVoxel, neighbours, timeStep)
        return currentVoxel.smokeConcentration + smokeTransferred + smokeGenerated
    }

    private fun smokeGenerated(currentVoxel: VoxelState, neighbours: List<VoxelState>, timeStep: Double): Double =
        neighbours.firstOrNull { it.isBelow(currentVoxel) && it.material.isBurning() }
            ?.let { generatedSmoke(it, timeStep) } ?: 0.0

    private fun smokeTransferred(currentVoxel: VoxelState, neighbours: List<VoxelState>, timeStep: Double): Double =
        neighbours.filter { it.material.transfersSmoke() && it != currentVoxel }
            .fold(0.0) { acc, neighbour ->
                acc + transferFactor(
                    currentVoxel, neighbour, neighbour.smokeConcentration < 1.0
                ) * smokeTransfer(currentVoxel.smokeConcentration, neighbour.smokeConcentration) // * timeStep

            }

    private fun generatedSmoke(n: VoxelState, timeStep: Double): Double {
        return n.material.smokeEmissionPerSecond!! * timeStep
    }

    fun smokeTransfer(a: Double, b: Double) = if (a > b) {
        min(a / 6, (1.0 - b) / 6)
    } else min(b / 6, (1.0 - a) / 6)

    /**
     * Coefficient + the direction of transfer
     */
    fun transferFactor(from: VoxelState, to: VoxelState, upperCanAccept: Boolean): Double = if (upperCanAccept) {
        when {
            to.isAbove(from) -> 1.0
            to.isBelow(from) -> 0.25
            else -> 0.5
        }
    } else {
        when {
            to.isAbove(from) -> 0.0
            to.isBelow(from) -> 1.0
            else -> 0.5
        }
    } * direction(from, to)

    private fun direction(from: VoxelState, to: VoxelState): Double =
        if (from.isBelow(to)) -1.0 else sign(to.smokeConcentration - from.smokeConcentration)

}