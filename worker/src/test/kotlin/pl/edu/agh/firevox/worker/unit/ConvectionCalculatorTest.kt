package pl.edu.agh.firevox.worker.unit

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.ConvectionCalculator
import java.math.BigDecimal
import java.math.RoundingMode

class ConvectionCalculatorTest : ShouldSpec({

    val convectionCalculator = ConvectionCalculator(0.01, 273.15)

    val metal = PhysicalMaterial(
        VoxelMaterial.METAL,
        density = 1000.0,
        baseTemperature = 1000.toKelvin(),
        thermalConductivityCoefficient = 1.0,
        convectionHeatTransferCoefficient = 1.0,
        specificHeatCapacity = 1000.0,
        ignitionTemperature = null,
        burningTime = null,
        timeToIgnition = null,
        autoignitionTemperature = null,
        effectiveHeatOfCombustion = null,
        smokeEmissionPerSecond = null,
        deformationTemperature = null,
        emissivity = 0.0,
    )

    val air = PhysicalMaterial(
        voxelMaterial = VoxelMaterial.AIR,
        density = 1.204,
        baseTemperature = 0.0.toKelvin(),
        thermalConductivityCoefficient = 0.024,
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

    should("calculate for all neighbour same temperature") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), metal, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), metal, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 15, 1), metal, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 16, 2), metal, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 14, 2), metal, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(16, 15, 2), metal, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(14, 15, 2), metal, 1000.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = convectionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result shouldBe 0
    }

    should("calculate for non-fluid with non-fluid neighbours") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), metal, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), metal, 0.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = convectionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (0.0).toNDecimal(5)
    }

    should("calculate for hot plate facing down") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), metal, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), air, 0.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = convectionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (-388.8).toNDecimal(5)
    }

    should("calculate for hot plate facing up") {
        // given
        val current = Triple(VoxelKey(15, 15, 1), metal, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 2), air, 0.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = convectionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (-388.8).toNDecimal(5)
    }

    should("calculate not overflow for big temp difference") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), air, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), air, 0.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 15, 1), air, 0.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = convectionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (-477.22698).toNDecimal(5)
    }

})


private fun Triple<VoxelKey, PhysicalMaterial, Double>.toVoxelState(): VoxelState = VoxelState(
    this.first,
    this.second,
    this.third,
    false, 0.0, 0, 0
)

private fun Double.toNDecimal(n: Int) = BigDecimal(this).setScale(n, RoundingMode.HALF_EVEN)

