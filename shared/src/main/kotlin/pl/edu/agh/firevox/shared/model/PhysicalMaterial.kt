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

    val density: Double, // unit kg/m^3
    val baseTemperature: Double, // unit K

    val thermalConductivityCoefficient: Double, // unit W/(mK)
    val convectionHeatTransferCoefficient: Double, // W/(m^2K)
    val specificHeatCapacity: Double, // unit J/(kg*K)

    val ignitionTemperature: Double?, // unit K
    val timeToIgnition: Double?, // unit s
    val autoignitionTemperature: Double?, // unit K

    val burningTime: Double?, // unit s
    val effectiveHeatOfCombustion: Double?, // unit J/s

    val smokeEmissionPerSecond: Double?, // no unit
    val deformationTemperature: Double?, // unit K

    val burnsCompletely: Boolean = false
) {

    @Id
    val id: Int = voxelMaterial.colorId

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
            baseTemperature = 20.0.toKelvin(),
            thermalConductivityCoefficient = 25.87,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 1.0061,
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