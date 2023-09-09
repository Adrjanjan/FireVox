package pl.edu.agh.firevox.shared.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name="material")
class PhysicalMaterial(
    @Column
    val voxelMaterial: VoxelMaterial,

    val density: Double, // unit kg/m^3
    val baseTemperature: Double, // unit K

    val thermalConductivityCoefficient: Double, // unit W/(mK)
    val convectionHeatTransferCoefficient: Double, //
    val specificHeatCapacity: Double, // unit J/(kg*K)

    val flashPointTemperature: Double, // unit K
    val burningTime: Double, // unit s
    val generatedEnergyDuringBurning: Double, // unit J/s

) {
    @Id
    private val id: Int = voxelMaterial.colorId

    fun isSolid() = this.voxelMaterial in listOf(
        VoxelMaterial.METAL,
        VoxelMaterial.GLASS_HEATED,
        VoxelMaterial.GLASS_HOT,
        VoxelMaterial.GLASS_VERY_HOT,
        VoxelMaterial.GLASS,
        VoxelMaterial.GLASS_HEATED,
        VoxelMaterial.GLASS_HOT,
        VoxelMaterial.GLASS_VERY_HOT,
        VoxelMaterial.CONCRETE,
        VoxelMaterial.WOOD,
        VoxelMaterial.WOOD_HEATED,
        VoxelMaterial.WOOD_BURNING,
        VoxelMaterial.WOOD_BURNT,
        VoxelMaterial.PLASTIC,
        VoxelMaterial.PLASTIC_HEATED,
        VoxelMaterial.PLASTIC_BURNING,
        VoxelMaterial.PLASTIC_BURNT,
        VoxelMaterial.TEXTILE,
        VoxelMaterial.TEXTILE_HEATED,
        VoxelMaterial.TEXTILE_BURNING,
        VoxelMaterial.TEXTILE_BURNT,
    )
}
