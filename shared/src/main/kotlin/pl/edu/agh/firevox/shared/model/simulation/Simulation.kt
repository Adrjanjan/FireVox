package pl.edu.agh.firevox.shared.model.simulation

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "simulation")
class Simulation(
    @Id
    val id: UUID = UUID.randomUUID(),
    val name: String,
    @OneToOne
    @JoinColumn(name = "simulationId")
    val parentModel: SingleModel,
    val creationDate: ZonedDateTime = ZonedDateTime.now(),
    val sizeX: Int,
    val sizeY: Int,
    val sizeZ: Int,
)

interface SimulationSizeView {
    fun getSizeX(): Int
    fun getSizeY(): Int
    fun getSizeZ(): Int
}

@Entity
@Table(name = "single_model")
class SingleModel(
    @Id
    val id: UUID = UUID.randomUUID(),
    val name: String,
    @OneToMany(mappedBy = "parentId")
    val childModels: List<SingleModel> = listOf(),
    @Column(name = "scale")
    val scale: Int = 1,
    @Column(name = "position_x")
    val positionX: Int? = 0,
    @Column(name = "position_y")
    val positionY: Int? = 0,
    @Column(name = "position_z")
    val positionZ: Int? = 0,
    @Column(name = "center_x")
    val centerX: Boolean? = false,
    @Column(name = "center_y")
    val centerY: Boolean? = false,
    @Column(name = "center_z")
    val centerZ: Boolean? = false,
    @Column(name = "flip_x")
    val flipX: Boolean? = false,
    @Column(name = "flip_y")
    val flipY: Boolean? = false,
    @Column(name = "flip_z")
    val flipZ: Boolean? = false,
    @Column(name = "rotate_x")
    val rotateX: Int? = 0,
    @Column(name = "rotate_y")
    val rotateY: Int? = 0,
    @Column(name = "rotate_z")
    val rotateZ: Int? = 0,
    @Column(name = "parent_id")
    val parentId: UUID? = null,
)