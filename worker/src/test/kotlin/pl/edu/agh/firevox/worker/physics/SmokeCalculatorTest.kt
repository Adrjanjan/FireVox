package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.bigdecimal.shouldBeZero
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow

class SmokeCalculatorTest : ShouldSpec({
    val timeStep = 0.1


    fun compareResults(first: Double, second: Double, expected: Double) {
        val f = first.toNDecimal(10)
        val s = second.toNDecimal(10)
        val e = expected.toNDecimal(10)
        f shouldBe e
        f shouldBe -s
    }

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

    val woodBurning = PhysicalMaterial(
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
        smokeEmissionPerSecond = 0.003, // 30%/(m^2/s)
        deformationTemperature = null,
        emissivity = 0.9
    )

    val voxelLength = 0.1
    val calculator = SmokeCalculator(voxelLength)

    context("calculate smoke transfer correctly") {
//
//        context("for no neighbours") {
//            val voxels = mutableListOf(
//                VoxelState(
//                    VoxelKey(0, 0, 0),
//                    material = smoke,
//                    temperature = 20.toKelvin(),
//                    wasProcessedThisIteration = false,
//                    smokeConcentration = 0.50,
//                    ignitingCounter = 0,
//                    burningCounter = 1,
//                )
//            )
//
//            val result = calculator.smokeTransferred(voxels[0], voxels)
//            compareResults(result, 0.0, 0.0)
//        }
//
//        context("for only upper empty neighbour available") {
//            val voxels = mutableListOf(
//                VoxelState(
//                    VoxelKey(0, 0, 0),
//                    material = smoke,
//                    temperature = 20.toKelvin(),
//                    wasProcessedThisIteration = false,
//                    smokeConcentration = 0.50,
//                    ignitingCounter = 0,
//                    burningCounter = 1,
//                ),
//                VoxelState(
//                    VoxelKey(0, 0, 1),
//                    material = air,
//                    temperature = 20.toKelvin(),
//                    wasProcessedThisIteration = false,
//                    smokeConcentration = 0.00,
//                    ignitingCounter = 0,
//                    burningCounter = 1,
//                )
//            )
//
//            val result = calculator.smokeTransferred(voxels[0], voxels)
//            val result2 = calculator.smokeTransferred(voxels[1], voxels)
//            compareResults(result, result2, -0.5 / 6)
//        }

        context("for only upper not fully empty neighbour available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.95,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.smokeTransferred(voxels[0], voxels)
            val result2 = calculator.smokeTransferred(voxels[1], voxels)
            val smokeIn = 0.5 * 0.5 / 6
            val smokeOut = -1.0 * (1 - 0.95) / 6
            val expected = smokeIn + smokeOut
            compareResults(result, result2, expected)
        }

        context("for only side neighbours available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.70,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, -1, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 1, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(1, 0, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(-1, 0, 0),
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.50,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.smokeTransferred(voxels[0], voxels)
            val result2 = calculator.smokeTransferred(voxels[1], voxels)
            compareResults(result, result2, -4 * 0.5 * 0.2 / 6)
        }

        context("for only below neighbour available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = smoke,
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

            val result = calculator.smokeTransferred(voxels[0], voxels)
            val result2 = calculator.smokeTransferred(voxels[1], voxels)
            compareResults(result, result2, -0.25 * 0.5 / 6)
        }

        context("for multiple cells smoke mass is conserved") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    smokeConcentration = 0.70,
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, -1, 0),
                    smokeConcentration = 0.50,
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 1, 0),
                    smokeConcentration = 0.50,
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(1, 0, 0),
                    smokeConcentration = 0.50,
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(-1, 0, 0),
                    smokeConcentration = 0.50,
                    material = smoke,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    smokeConcentration = 0.00,
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                ),
                VoxelState(
                    VoxelKey(0, 0, -1),
                    smokeConcentration = 0.00,
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.smokeTransferred(voxels[0], voxels)
            val result2 = calculator.smokeTransferred(voxels[1], voxels)
            compareResults(result, result2, (0.5 * 4 * 0.3) / 6 - (0.5 * 4 * 0.5 + 1.0 * 0.7 + 0.25 * 0.7) / 6)
        }


        context("for only lower available") {
            val voxels = mutableListOf(
                VoxelState(
                    VoxelKey(0, 0, 0),
                    material = smoke,
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
                ),
                VoxelState(
                    VoxelKey(0, 0, 1),
                    material = air,
                    temperature = 20.toKelvin(),
                    wasProcessedThisIteration = false,
                    smokeConcentration = 0.99999,
                    ignitingCounter = 0,
                    burningCounter = 1,
                )
            )

            val result = calculator.smokeTransferred(voxels[0], voxels)
            val result2 = calculator.smokeTransferred(voxels[1], voxels)
            compareResults(result, result2, (-1.0*0.5)/6)
        }
    }
})

private fun Double.toNDecimal(n: Int) = BigDecimal(this).setScale(n, RoundingMode.HALF_EVEN)