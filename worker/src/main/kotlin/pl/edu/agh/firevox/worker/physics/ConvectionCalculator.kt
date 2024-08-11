package pl.edu.agh.firevox.worker.physics

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelState
import pl.edu.agh.firevox.worker.service.InvalidSimulationState
import kotlin.math.abs
import kotlin.math.pow

@Service
class ConvectionCalculator(
    @Value("\${firevox.voxel.size}") val voxelLength: Double,
    @Value("\${firevox.voxel.ambient}") val ambientTemperature: Double,
) {

    val characteristicLength = voxelLength / 6 // equation for cubes

    /**
     * Only natural convection is included
     *
     * T_final = timestep * dQ / (mass * specific_heat_capacity)
     * mass = density * l^3
     * dQ = area * dT * h_avg
     * area = l^2
     * po skroceniu
     * Q = dT * h_avg
     * dT = timestep * Q / ( density * l * specific_heat_capacity)
     **/
    fun calculate(
        voxel: VoxelState, voxels: List<VoxelState>, timeStep: Double, voxelsToSend: MutableSet<VoxelKey>
    ): Double {
        val lower = voxels.firstOrNull {
            it.isBelow(voxel) && canConvect(it, voxel) && it.temperature != voxel.temperature
        }
        val upper = voxels.firstOrNull {
            it.isAbove(voxel) && canConvect(it, voxel) && it.temperature != voxel.temperature
        }
        val currentMaterial = voxel.material
        val constants = (currentMaterial.density * voxelLength * currentMaterial.specificHeatCapacity)
        val heatExchangeWithUpper = convectiveHeat(upper, voxel, voxelsToSend)
        val heatExchangeWithLower = convectiveHeat(lower, voxel, voxelsToSend)
        return timeStep * (heatExchangeWithUpper + heatExchangeWithLower) / constants
    }

    private fun canConvect(first: VoxelState, second: VoxelState): Boolean = when {
        first.material.isSolid() && second.material.isFluid() -> true
        first.material.isFluid() && second.material.isSolid() -> true
        first.material.isFluid() && second.material.isFluid() -> true
        else -> false
    }

    // <- dT * h_avg == dQ / area
    private fun convectiveHeat(
        other: VoxelState?, current: VoxelState, voxelsToSend: MutableSet<VoxelKey>
    ) = if (current.material.isSolid() && other?.material?.isSolid() == true) 0.0 else {
        val heat = other
            ?.let { (it.temperature - current.temperature) * calculateHeatTransferCoefficient(it, current) }
            ?.also { voxelsToSend.add(other.key) } ?: 0.0
        heat
    }

    // https://www.engineersedge.com/physics/viscosity_of_air_dynamic_and_kinematic_14483.htm
    // https://en.wikipedia.org/wiki/Heat_transfer_coefficient#cite_ref-5
    private fun calculateHeatTransferCoefficient(it: VoxelState, current: VoxelState): Double {
        val result = when {
            current.temperature > it.temperature && it.isAbove(current) -> hotPlateFacingUp(it, current)
            current.temperature > it.temperature && it.isBelow(current) -> hotPlateFacingDown(it, current)
            current.temperature < it.temperature && it.isBelow(current) -> hotPlateFacingUp(current, it)
            current.temperature < it.temperature && it.isAbove(current) -> hotPlateFacingDown(current, it)
            else -> throw InvalidSimulationState("Invalid convection configuration") // unreachable
        }
        return result
    }

    private fun hotPlateFacingUp(coldUp: VoxelState, hotBelow: VoxelState): Double {
        val fluid = if (coldUp.material.isFluid()) coldUp else hotBelow
        val k = fluid.material.thermalConductivityCoefficient
        val Ra = rayleighNumber(coldUp, hotBelow, fluid, k)
        val result = if(Ra <= 1e7) {
            k * 0.54 * Ra.pow(1 / 4) / characteristicLength
        } else {
            k * 0.15 * Ra.pow(1 / 3) / characteristicLength
        }
        return result
    }

    private fun hotPlateFacingDown(coldDown: VoxelState, hotAbove: VoxelState): Double {
        val fluid = if (coldDown.material.isFluid()) coldDown else hotAbove
        val k = fluid.material.thermalConductivityCoefficient
        val Ra = rayleighNumber(coldDown, hotAbove, fluid, k)
        val result = k * 0.52 * Ra.pow(1 / 5) / characteristicLength
        return result
    }

    // https://en.wikipedia.org/wiki/Rayleigh_number
    // 9.81 * beta * dT * characteristicLength.pow(3) / (kinematicViscosity.pow(2) * alpha)
    // alpha = k / (fluid.material.density * fluid.material.specificHeatCapacity)
    // beta = 1/avg(cold.temp, hot.temp)
    // kinematicViscosity = dynamicViscosity/density
    private fun rayleighNumber(
        coldDown: VoxelState, hotAbove: VoxelState, fluid: VoxelState, k: Double
    ): Double {
        val dT = abs(coldDown.temperature - hotAbove.temperature)
        val dynamicViscosity = dynamicViscosity(fluid.temperature)
        val filmTemp = (coldDown.temperature + hotAbove.temperature) / 2
        val density = fluid.material.density
        val Cp = fluid.material.specificHeatCapacity
        val g = 9.81
        val Ra = characteristicLength.pow(3) * density.pow(2) * Cp * g * dT / (dynamicViscosity * k * filmTemp)
        return Ra
    }

    // sutherland formula
    // mi = mi_t0 (T/t0)^(3/2) * (t0+C)/(T+C)
    // v = mi/density
    private fun dynamicViscosity(temp: Double): Double {
        // Sutherland's Formula
        val mi0 = 1.716e-5
        val t0 = 273.15
        val C = 110.4
        val mi = mi0 * (temp / t0).pow(3 / 2) * (t0 + C) / (temp + C)
        return mi
    }

    private fun densityFromTemperatureForAir(temp: Double): Double {
        val atmPressure = 101325
        val R = 287.05
        return atmPressure / (temp * R)
    }
}

// podsumowanie
// Ra wydaje się liczy OK
// może dynamicViscosity jest do poprawienia