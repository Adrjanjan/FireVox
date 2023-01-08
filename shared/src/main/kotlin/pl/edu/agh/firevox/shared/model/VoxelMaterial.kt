package pl.edu.agh.firevox.shared.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class VoxelMaterial(

    @Id
    val id: Int,

)
