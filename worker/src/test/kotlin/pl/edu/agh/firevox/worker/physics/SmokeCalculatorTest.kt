package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.service.VoxelState

class SmokeCalculatorTest : ShouldSpec({
    val timeStep = 0.1

    val air = PhysicalMaterial(
        voxelMaterial = VoxelMaterial.AIR,
        density = 1.204,
        baseTemperature = 25.0.toKelvin(),
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

    val smoke = PhysicalMaterial(
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

    val wood = PhysicalMaterial(
        voxelMaterial = VoxelMaterial.WOOD_BURNING,
        density = 400.0,
        baseTemperature = 20.0.toKelvin(),
        thermalConductivityCoefficient = 0.3,
        convectionHeatTransferCoefficient = 0.0,
        specificHeatCapacity = 2390.0,
        ignitionTemperature = 250.toKelvin(),
        timeToIgnition = 0.5, //1.0, // TODO
        autoignitionTemperature = 2000.toKelvin(),
        burningTime = 1.4, // TODO
        effectiveHeatOfCombustion = 1500.0,
        smokeEmissionPerSecond = 0.3,
        deformationTemperature = null,
        emissivity = 0.9
    )
    val calculator = SmokeCalculator()

    context("calculate smoke transfer correctly") {

        context("for no neighbours") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.50
        }

        context("for only upper empty neighbour available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.00,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.5 - 0.5 / 6
        }

        context("for only upper not fully empty neighbour available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.90,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.5 - 0.1 / 6
        }

        context("for only side neighbours available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.70,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, -1, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 1, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(1, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(-1, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.7 - (4 * 0.5 * (0.5 / 6))
        }

        context("for only below neighbour available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, -1),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.00,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.5 - 0.25 * 0.5 / 6
        }

        context("for multiple cells smoke mass is conserved") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.70,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, -1, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 1, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(1, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(-1, 0, 0),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.00,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe 0.7 - (4 * 0.5 * (0.5 / 6) + 1 * 0.7 / 6)
        }
    }

    context("calculate smoke generation") {
        should("return correct value") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = wood,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.0,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.calculate(voxels[0], voxels, timeStep, 0, mutableSetOf())
            result shouldBe wood.smokeEmissionPerSecond!! * timeStep
        }
    }
})
