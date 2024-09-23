package pl.edu.agh.firevox.shared.model

import jakarta.persistence.*
import org.hibernate.Hibernate
import java.util.*

@Entity
@Table(
    name = "voxels",
)
data class Voxel(
    @EmbeddedId
    val key: VoxelKey,

    // even
    @ManyToOne
    var evenIterationMaterial: PhysicalMaterial,
    var evenIterationTemperature: Double,

    // odd
    @ManyToOne
    var oddIterationMaterial: PhysicalMaterial,
    var oddIterationTemperature: Double,

    var evenSmokeConcentration: Double = 0.0,
    var oddSmokeConcentration: Double = 0.0,

    var ignitingCounter: Int = 0,
    var burningCounter: Int = 0,
    var isBoundaryCondition: Boolean = false,
    var ambienceInsulated: Boolean = false,
    var lastProcessedIteration : Int = -1,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Voxel

        return key == other.key
    }

    override fun hashCode(): Int = Objects.hash(key);

    fun Double.toCelsius() = this.minus(273.15)
    override fun toString(): String {
        return "Voxel(key=$key, even=$evenSmokeConcentration $evenIterationMaterial, odd=$oddSmokeConcentration $oddIterationMaterial)"
    }

    fun toVoxelState(iteration: Int) = if (iteration % 2 == 0)
        VoxelState(
            this.key,
            this.evenIterationMaterial,
            this.evenIterationTemperature,
            this.lastProcessedIteration >= iteration,
            this.evenSmokeConcentration,
            this.ignitingCounter,
            this.burningCounter,
            this.ambienceInsulated,
        ) else
        VoxelState(
            this.key,
            this.oddIterationMaterial,
            this.oddIterationTemperature,
            this.lastProcessedIteration >= iteration,
            this.oddSmokeConcentration,
            this.ignitingCounter,
            this.burningCounter,
            this.ambienceInsulated,
        )
}


data class VoxelState(
    val key: VoxelKey,
    var material: PhysicalMaterial,
    var temperature: Double,
    val wasProcessedThisIteration: Boolean,
    val smokeConcentration: Double,
    var ignitingCounter: Int = 0,
    var burningCounter: Int = 0,
    var ambienceInsulated: Boolean = false,
) {
    override fun toString(): String {
        return "VoxelState(key=$key, temperature=${temperature.toCelsius()}, material=${material.voxelMaterial.name}, smoke=$smokeConcentration, burningCounter=${burningCounter}, ignitingCounter=$ignitingCounter, ambienceInsulated=$ambienceInsulated)\n"
    }

    fun isAbove(other: VoxelState) = this.key.isAbove(other.key)

    fun isBelow(other: VoxelState) = this.key.isBelow(other.key)
}
