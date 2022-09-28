package pl.edu.agh.firevox.worker.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.worker.repository.VoxelRepository
import java.util.*

@Service
class WorkerService {

    @Autowired
    lateinit var voxelRepository: VoxelRepository

    fun getVoxel(voxelId: UUID) = voxelRepository.findById(voxelId).orElseGet { null }

    fun delete(voxelId: UUID) = voxelRepository.deleteById(voxelId)
}
