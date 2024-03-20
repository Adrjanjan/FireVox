package pl.edu.agh.firevox.shared.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView

class VoxelsChunkTest : ShouldSpec({

    val material = PhysicalMaterial(
        voxelMaterial = VoxelMaterial.SMOKE,
        density = 1.4,
        baseTemperature = 40.0.toKelvin(),
        thermalConductivityCoefficient = 25.87,
        convectionHeatTransferCoefficient = 38.0,
        specificHeatCapacity = 1015.0,
        ignitionTemperature = null,
        timeToIgnition = null,
        autoignitionTemperature = null,
        burningTime = null,
        effectiveHeatOfCombustion = null,
        smokeEmissionPerSecond = null,
        deformationTemperature = null
    )

    val chunk = VoxelsChunk(
        VoxelKey(0, 0, 0),
        VoxelKey(4, 4, 4),
        SimulationSizeView(4, 4, 4)
    ).also {
        it.voxels = Array(it.model.sizeX) { x ->
            Array(it.model.sizeY) { y ->
                Array(it.model.sizeZ) { z ->
                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = material,
                        evenIterationTemperature = 25.toKelvin(),
                        oddIterationMaterial = material,
                        oddIterationTemperature = 25.toKelvin(),
                        isBoundaryCondition = false
                    )
                }
            }
        }

    }

    context("return correct NEWSUL neighbours for each key") {

        should("correct of corner (0, 0, 0)"){
            val result = chunk.neighbours(VoxelKey(0, 0, 0), NeighbourhoodType.N_E_W_S_U_L_).first

            result.size shouldBe 3
            result.map { it.key } shouldContainAll listOf(
                VoxelKey(1, 0, 0),
                VoxelKey(0, 1, 0),
                VoxelKey(0, 0, 1),
            )
        }

        should("correct of middle (2, 2, 2)"){
            val result = chunk.neighbours(VoxelKey(2, 2, 2), NeighbourhoodType.N_E_W_S_U_L_).first

            result.size shouldBe 6
            result.map { it.key } shouldContainAll listOf(
                VoxelKey(1, 2, 2),
                VoxelKey(3, 2, 2),
                VoxelKey(2, 1, 2),
                VoxelKey(2, 3, 2),
                VoxelKey(2, 2, 1),
                VoxelKey(2, 2, 3),
            )
        }

        should("correct of wall (0, 1, 1)"){
            val result = chunk.neighbours(VoxelKey(0, 1, 1), NeighbourhoodType.N_E_W_S_U_L_).first

            result.size shouldBe 5
            result.map { it.key } shouldContainAll listOf(
                VoxelKey(1, 1, 1),
                VoxelKey(0, 0, 1),
                VoxelKey(0, 2, 1),
                VoxelKey(0, 1, 0),
                VoxelKey(0, 1, 2),
            )
        }

    }
})
