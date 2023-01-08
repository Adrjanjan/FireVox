package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.*

@Service
class WorkerService(
    private val voxelRepository: CustomVoxelRepository
//    var materialRepository: MaterialRepository,
) {

    fun calculate(voxelKey: VoxelKey): List<Voxel> {
        TODO()
    }

}
