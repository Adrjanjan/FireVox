package pl.edu.agh.firevox.shared.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name="material")
class PhysicsMaterial(
    @Id
    val id: Int = TODO(), //same as in .vox files

    @OneToOne
    val voxelMaterial: VoxelMaterial,


    val baseTemperature: Double,
    val flashPointTemperature: Double,
    val thermalConductivityCoefficient: Double,
    val convectionHeatTransferCoefficient: Double,
    val heatCapacity: Double,
    // TODO add other values related to amount of energy generated while burning/smoke generated etc
)
