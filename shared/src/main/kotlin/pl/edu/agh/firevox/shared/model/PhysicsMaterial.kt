package pl.edu.agh.firevox.shared.model

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToOne
import javax.persistence.Table

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
