package pl.edu.agh.firevox.shared.model

import jakarta.persistence.*
import org.hibernate.Hibernate
import java.util.*

@Entity
@Table(
    indexes = [
        Index(name = "xyz", columnList = "x, y, z", unique = true),
        Index(name = "x", columnList = "x", unique = false),
        Index(name = "y", columnList = "y", unique = false),
        Index(name = "z", columnList = "z", unique = false),
        Index(name = "xyz_odd", columnList = "x, y, z, oddIterationNumber", unique = true),
        Index(name = "xyz_even", columnList = "x, y, z, evenIterationNumber", unique = true),
    ]
)
data class Voxel(
    @EmbeddedId
    val key: VoxelKey,

    // even
    var evenIterationNumber: Int,
    @ManyToOne
    var evenIterationMaterial: PhysicalMaterial,
    var evenIterationTemperature: Double,

    // odd
    var oddIterationNumber: Int,
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
        return "Voxel(key=$key, evenIterationNumber=$evenIterationNumber, evenIterationMaterial=$evenIterationMaterial, evenIterationTemperature=${evenIterationTemperature.toCelsius()}, oddIterationNumber=$oddIterationNumber, oddIterationMaterial=$oddIterationMaterial, oddIterationTemperature=${oddIterationTemperature.toCelsius()}, isBoundaryCondition=$isBoundaryCondition)"
    }
}