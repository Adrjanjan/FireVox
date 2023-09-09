package pl.edu.agh.firevox.shared.model.thermometer

import jakarta.persistence.*
import pl.edu.agh.firevox.shared.model.VoxelKey

@Entity
class VirtualThermometer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val voxelKey: VoxelKey,
    var measurements: String = ""
)
