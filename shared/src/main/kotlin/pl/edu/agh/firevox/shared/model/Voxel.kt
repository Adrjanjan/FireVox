package pl.edu.agh.firevox.shared.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.ManyToOne
import org.hibernate.Hibernate
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.Embedded

@Entity
data class Voxel(
    @EmbeddedId
    val voxelKey: VoxelKey,

    @ManyToOne
    var physicalMaterial: PhysicalMaterial,

    @Embedded
    val currentProperties: StateProperties,

    @Embedded
    var nextProperties: StateProperties?
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
        return this::class.simpleName + "(EmbeddedId = $voxelKey , currentProperties = $currentProperties , nextProperties = $nextProperties )"
    }

}

@Embeddable
data class StateProperties(
    var iterationNumber: Int,
    var heat: Double,
    var temperature: Double,

)