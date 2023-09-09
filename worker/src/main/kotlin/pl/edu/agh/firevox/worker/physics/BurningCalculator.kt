package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.Voxel
import kotlin.math.pow


//@Service
//class BurningCalculator(
//    @Value("${firevox.voxel.size}") val voxelLength: Double,
//) {
//
//
//    /**
//     * newTemp = current.Temp + deltaTemp
//     * deltaTemp = deltaQ/mC
//     * m = material.density * V
//     * C = material.heatCapacity
//     * deltaQ = delta T * generatedEnergyPerSecond
//    **/
//    override fun calculate(voxel: Voxel, voxels: List<Voxel>, timeStep: Double): ?? {
//        val current = voxel.currentProperties
//        val currentMaterial = current.material
//        val volume: Double = voxelLength.pow(3)
//        val mass = currentMaterial.density * volume
//
//        val newTemp = (timeStep * currentMaterial.generatedEnergyDuringBurning) / (mass * currentMaterial.specificHeatCapacity)
//
//        //TODO add logic that will change the material to burned one based on burning time
//
//
//        return StateProperties(
//            current.iterationNumber + 1,
//            currentMaterial,
//            current.temperature + newTemp
//        )
//    }
//
//
//}