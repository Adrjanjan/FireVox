package pl.edu.agh.firevox.shared.model

import jakarta.annotation.PostConstruct
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration

@Entity
@Table(name = "materials")
class PhysicalMaterial(
    @Column(nullable = false)
    val voxelMaterial: VoxelMaterial,

    var density: Double, // unit kg/m^3
    var baseTemperature: Double, // unit K

    var thermalConductivityCoefficient: Double, // unit W/(mK)
    @Deprecated("Dynamic value", level = DeprecationLevel.HIDDEN)
    var convectionHeatTransferCoefficient: Double, // W/(m^2K)
    var specificHeatCapacity: Double, // unit J/(kg*K)

    var ignitionTemperature: Double?, // unit K
    var timeToIgnition: Double?, // unit s
    var autoignitionTemperature: Double?, // unit K

    var burningTime: Double?, // unit s
    var effectiveHeatOfCombustion: Double?, // unit J/s

    var smokeEmissionPerSecond: Double?, // (%/(m^2/s))
    var deformationTemperature: Double?, // unit K

    var burnsCompletely: Boolean = false,
    var emissivity: Double = 0.0,
) {

    @Id
    val id: Int = voxelMaterial.colorId + 1000

    fun isSolid() = this.voxelMaterial in listOf(
        VoxelMaterial.METAL,
        VoxelMaterial.GLASS,
        VoxelMaterial.CONCRETE,
        VoxelMaterial.WOOD,
        VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.WOOD_BURNT,
        VoxelMaterial.PLASTIC,
        VoxelMaterial.PLASTIC_BURNING,
        VoxelMaterial.PLASTIC_BURNT,
        VoxelMaterial.TEXTILE,
        VoxelMaterial.TEXTILE_BURNING,
        VoxelMaterial.TEXTILE_BURNT,
    )

    fun isFlammable() = this.voxelMaterial in listOf(
        VoxelMaterial.WOOD,
        VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.PLASTIC,
        VoxelMaterial.PLASTIC_BURNING,
        VoxelMaterial.TEXTILE,
        VoxelMaterial.TEXTILE_BURNING,
    )


    fun isBurning() = this.voxelMaterial in listOf(
        VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.PLASTIC_BURNING,
        VoxelMaterial.TEXTILE_BURNING,
    )

    fun isFluid() = this.voxelMaterial in listOf(
        VoxelMaterial.AIR,
        VoxelMaterial.WATER,
        VoxelMaterial.SMOKE,
        VoxelMaterial.FLAME,
    )

    fun transfersSmoke() = this.voxelMaterial in listOf(
        VoxelMaterial.AIR,
        VoxelMaterial.SMOKE,
        VoxelMaterial.FLAME,
    )

    fun canContainOxygen() = this.voxelMaterial in listOf(
        VoxelMaterial.AIR,
        VoxelMaterial.SMOKE,
        VoxelMaterial.FLAME,
    )

    fun isLiquid() = this.voxelMaterial in listOf(
        VoxelMaterial.WATER,
    )

    override fun toString(): String {
        return voxelMaterial.name
    }

}

@Configuration
class MaterialsConfig(
    private val physicalMaterialRepository: PhysicalMaterialRepository
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostConstruct
    fun initMaterials() {
        val materials = mutableListOf<PhysicalMaterial>()

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.AIR,
            density = 1.204,
            baseTemperature = 25.0.toKelvin(),
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
        ).let(materials::add)

        PhysicalMaterial(
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
        ).let(materials::add)

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.FLAME,
            density = 0.204,
            baseTemperature = 150.0.toKelvin(),
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
        ).let(materials::add)

        listOf(VoxelMaterial.WOOD, VoxelMaterial.WOOD_BURNING, VoxelMaterial.WOOD_BURNT).map {
            PhysicalMaterial(
                voxelMaterial = it,
                density = 400.0,
                baseTemperature = 20.0.toKelvin(),
                thermalConductivityCoefficient = 0.3,
                convectionHeatTransferCoefficient = 0.0,
                specificHeatCapacity = 2390.0,
                ignitionTemperature = 250.toKelvin(),
                timeToIgnition = 0.5 , //1.0, // TODO
                autoignitionTemperature = 2000.toKelvin(),
                burningTime = 1.4, // TODO
                effectiveHeatOfCombustion = 1500.0,
                smokeEmissionPerSecond = 0.3,
                deformationTemperature = null,
                emissivity = 0.9
            )
        }.let(materials::addAll)

        listOf(VoxelMaterial.PLASTIC, VoxelMaterial.PLASTIC_BURNING, VoxelMaterial.PLASTIC_BURNT).map {
            PhysicalMaterial(
                voxelMaterial = it,
                density = 300.0,
                baseTemperature = 20.0.toKelvin(),
                thermalConductivityCoefficient = 0.174,
                convectionHeatTransferCoefficient = 0.0,
                specificHeatCapacity = 5200.0,
                ignitionTemperature = 120.toKelvin(),
                timeToIgnition = 0.4, // TODO
                autoignitionTemperature = 750.toKelvin(),
                burningTime = 0.6, // TODO
                effectiveHeatOfCombustion = 500.0,
                smokeEmissionPerSecond = 0.8,
                deformationTemperature = null,
                emissivity = 0.9
            )
        }.let(materials::addAll)

        listOf(VoxelMaterial.TEXTILE, VoxelMaterial.TEXTILE_BURNING, VoxelMaterial.TEXTILE_BURNT).map {
            PhysicalMaterial(
                voxelMaterial = it,
                density = 400.0,
                baseTemperature = 20.0.toKelvin(),
                thermalConductivityCoefficient = 0.3,
                convectionHeatTransferCoefficient = 0.0,
                specificHeatCapacity = 2390.0,
                ignitionTemperature = 350.toKelvin(),
                timeToIgnition = 0.2, // TODO
                autoignitionTemperature = 1400.toKelvin(),
                burningTime = 0.4, // TODO
                effectiveHeatOfCombustion = 1500.0,
                smokeEmissionPerSecond = 1.0,
                deformationTemperature = null
            )
        }.let(materials::addAll)

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.METAL,
            density = 2700.0,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 235.0,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 897.0,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
            emissivity = 0.1,
        ).let(materials::add)

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.GLASS,
            density = 2700.0,
            baseTemperature = 20.0.toKelvin(),
            thermalConductivityCoefficient = 1.0,
            convectionHeatTransferCoefficient = 55.0,
            specificHeatCapacity = 8400.0,
            ignitionTemperature = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            burningTime = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = 150.0,
            emissivity = 0.89
        ).let(materials::add)

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.CONCRETE,
            density = 2392.0,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 2.071,
            convectionHeatTransferCoefficient = 55.6,
            specificHeatCapacity = 936.3,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
            emissivity = 0.9,
        ).let(materials::add)

        PhysicalMaterial(
            voxelMaterial = VoxelMaterial.WATER,
            density = 0.999,
            baseTemperature = 20.0.toKelvin(),
            thermalConductivityCoefficient = 0.6,
            convectionHeatTransferCoefficient = 1000.0,
            specificHeatCapacity = 4190.0,
            ignitionTemperature = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            burningTime = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null
        ).let(materials::add)

        physicalMaterialRepository.saveAll(materials)
    }

}