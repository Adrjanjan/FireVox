package pl.edu.agh.firevox.shared.model.simulation.counters

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "counters")
class Counter(
    @Id
    val id: CounterId,
    val count: Long,
)

enum class CounterId {
    CURRENT_ITERATION,
    MAX_ITERATIONS,

    PROCESSED_VOXEL_COUNT,
    CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT,
    NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT,

    PROCESSED_RADIATION_PLANES_COUNT,
    CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
    NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
}
