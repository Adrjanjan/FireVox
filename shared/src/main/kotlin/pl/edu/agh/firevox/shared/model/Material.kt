package pl.edu.agh.firevox.shared.model

import java.util.*
import javax.persistence.*

@Entity
@Table(name="material")
class Material(
    @Id
    val id: UUID = TODO(),

    val baseTemperature: Double,
    val flashPointTemperature: Double,
    // TODO add other values related to amount of energy generated while burning/smoke generated etc
)
