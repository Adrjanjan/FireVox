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

    val density: Double,
    val baseTemperature: Double,

    val thermalConductivityCoefficient: Double, // unit W/(mK)
    val convectionHeatTransferCoefficient: Double,
    val specificHeatCapacity: Double, // unit

    val flashPointTemperature: Double, // unit *C
    val burningTime: Double, // val calories: Double, instead? // unit s
    val generatedEnergyDuringBurning: Double, // unit J/s

    // TODO add other values related to amount of energy generated while burning/smoke generated etc
) {
    @Id
    private val id: Int = voxelMaterial.colorId
}
