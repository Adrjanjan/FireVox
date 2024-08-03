package pl.edu.agh.firevox.worker.unit

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.worker.physics.ConductionCalculator
import java.math.BigDecimal
import java.math.RoundingMode

class ConductionCalculatorTest : ShouldSpec({

    val conductionCalculator = ConductionCalculator(0.01)

    val baseMaterial = PhysicalMaterial(
        VoxelMaterial.METAL,
        density = 1000.0,
        baseTemperature = 1000.toKelvin(),
        thermalConductivityCoefficient = 1.0,
        convectionHeatTransferCoefficient = 1.0,
        specificHeatCapacity = 0.001,
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

    should("calculate for no neighbour") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf<VoxelState>()

        // WHEN
        val result = conductionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result shouldBe 0
    }

    should("calculate for all neighbour same temperature") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 15, 1), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 16, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(15, 14, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(16, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
            Triple(VoxelKey(14, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = conductionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result shouldBe 0
    }

    should("calculate for single neighbour different temperature same material") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), baseMaterial, 500.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = conductionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (-2_500_000.0).toNDecimal(5)
    }

    should("calculate for single neighbour different temperature different material") {
        // given
        val current = Triple(VoxelKey(15, 15, 2), baseMaterial, 1000.0.toKelvin()).toVoxelState()
        val neighbours = listOf(
            Triple(VoxelKey(15, 15, 3), air, 500.0.toKelvin()).toVoxelState(),
        )

        // WHEN
        val result = conductionCalculator.calculate(current, neighbours, 0.5, mutableSetOf())
        //then
        result.toNDecimal(5) shouldBe (-3.35875e7).toNDecimal(5)
    }

})

private fun Triple<VoxelKey, PhysicalMaterial, Double>.toVoxelState(): VoxelState = VoxelState(
    this.first,
    this.second,
    this.third,
    false, 0.0, 0, 0
)

private fun Double.toNDecimal(n: Int) = BigDecimal(this).setScale(n, RoundingMode.HALF_EVEN)
