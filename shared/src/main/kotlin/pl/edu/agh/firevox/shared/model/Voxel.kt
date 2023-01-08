package pl.edu.agh.firevox.shared.model

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import java.util.*

@Entity
data class Voxel(
    @EmbeddedId
    val voxelKey: VoxelKey,
    var currentIteration: Int = 0,

    val energy: Double,
    val temperature: Double,

    @ManyToOne
    var physicsMaterial: PhysicsMaterial,

// TODO add other properties
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Voxel

        return voxelKey == other.voxelKey
    }

    override fun hashCode(): Int = Objects.hash(voxelKey);

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(EmbeddedId = $voxelKey )"
    }
}