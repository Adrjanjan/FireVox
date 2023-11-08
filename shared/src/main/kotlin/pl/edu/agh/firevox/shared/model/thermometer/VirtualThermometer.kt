package pl.edu.agh.firevox.shared.model.thermometer

import jakarta.persistence.*
import pl.edu.agh.firevox.shared.model.VoxelKey

@Entity
@Table(indexes = [
    Index(name = "key_idx", columnList = "x, y, z", unique = true)
])
class VirtualThermometer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val voxelKey: VoxelKey,
    @Column(columnDefinition = "TEXT")
    var measurements: String = ""
)
