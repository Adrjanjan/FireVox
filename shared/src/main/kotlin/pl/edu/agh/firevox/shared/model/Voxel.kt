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

    var isBoundaryCondition: Boolean = false,
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
        return "Voxel(key=$key, evenIterationMaterial=$evenIterationMaterial, evenIterationTemperature=${evenIterationTemperature.toCelsius()}, oddIterationMaterial=$oddIterationMaterial, oddIterationTemperature=${oddIterationTemperature.toCelsius()}, isBoundaryCondition=$isBoundaryCondition)"
    }
}