package pl.edu.agh.firevox.model

import java.time.Instant
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class Simulation(
    @Id
    val id: UUID,
    val name: String,
    val creationDate: Instant,

)