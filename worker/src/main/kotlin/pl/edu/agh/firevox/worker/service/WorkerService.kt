package pl.edu.agh.firevox.worker.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelDto
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelRepository

@Service
class WorkerService(
    var voxelRepository: VoxelRepository,
//    var materialRepository: MaterialRepository,
){

    fun calculate(voxelKey:VoxelKey) : List<VoxelDto> {
        TODO()
    }

}
