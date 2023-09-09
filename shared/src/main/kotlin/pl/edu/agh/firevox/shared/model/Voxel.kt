package pl.edu.agh.firevox.shared.model

import jakarta.persistence.*
import org.hibernate.Hibernate
import java.util.*

@Entity
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

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(EmbeddedId = $key , currentIterationNumber = $evenIterationNumber , currentMaterial = $evenIterationMaterial , currentTemperature = $evenIterationTemperature )"
    }

}