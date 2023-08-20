package pl.edu.agh.firevox.shared.model

import org.hibernate.Hibernate
import java.util.*
import javax.persistence.*

@Entity
data class Voxel(
    @EmbeddedId
    val voxelKey: VoxelKey,

    @Embedded
    @AttributeOverride(name = "iterationNumber", column = Column(name = "current_iteration_number"))
    @AttributeOverride(name = "material", column = Column(name = "current_material"))
    @AttributeOverride(name = "burningTick", column = Column(name = "current_burning_tick"))
    val currentProperties: StateProperties,

    @Embedded
    @AttributeOverride(name = "iterationNumber", column = Column(name = "next_iteration_number"))
    @AttributeOverride(name = "material", column = Column(name = "next_material"))
    @AttributeOverride(name = "burningTick", column = Column(name = "next_burning_tick"))
    var nextProperties: StateProperties // same as currentProperties in first frame
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

    fun isBelow(other: Voxel) = voxelKey.isBelow(other.voxelKey)
    fun isAbove(other: Voxel) = voxelKey.isAbove(other.voxelKey)

}

@Embeddable
data class StateProperties(
    var iterationNumber: Int,
    var material: VoxelMaterial,
    var burningTick: Int = 0
)