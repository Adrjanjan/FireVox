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
    ]
)
data class Voxel(
    @EmbeddedId
    val key: VoxelKey,

    @Version
    val version: Int = 0,
    // current
    var evenIterationNumber: Int,
    @ManyToOne
    var evenIterationMaterial: PhysicalMaterial,
    var evenIterationTemperature: Double,

    // next
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

    fun isBelow(other: Voxel) = key.isBelow(other.key)
    fun isAbove(other: Voxel) = key.isAbove(other.key)

    fun Double.toCelsius() = this.minus(273.15)
    override fun toString(): String {
        return "Voxel(key=$key, evenIterationNumber=$evenIterationNumber, evenIterationMaterial=$evenIterationMaterial, evenIterationTemperature=${evenIterationTemperature.toCelsius()}, oddIterationNumber=$oddIterationNumber, oddIterationMaterial=$oddIterationMaterial, oddIterationTemperature=${oddIterationTemperature.toCelsius()}, isBoundaryCondition=$isBoundaryCondition)"
    }
}