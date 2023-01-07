package pl.edu.agh.firevox.shared.model

import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
data class Voxel(
    @EmbeddedId
    val voxelKey: VoxelKey,
    var currentIteration: Int = 0,

    val energy: Double,
    val temperature: Double,
    @ManyToOne
    val material: Material,
// TODO add other properties
)