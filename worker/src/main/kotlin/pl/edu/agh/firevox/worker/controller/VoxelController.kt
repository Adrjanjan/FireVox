package pl.edu.agh.firevox.worker.controller

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import pl.edu.agh.firevox.shareddto.model.VoxelDto
import pl.edu.agh.firevox.worker.service.WorkerService
import java.util.*

@RestController
@RequestMapping("/voxel")
class VoxelController {

    @Autowired
    lateinit var workerService: WorkerService

    @GetMapping("/{voxelId}")
    fun getVoxel(@PathVariable voxelId: UUID): VoxelDto? {
        LOGGER.info("[Get Voxel] for user $voxelId")
        return workerService.getVoxel(voxelId)
    }

    @DeleteMapping("/{voxelId}")
    fun deleteVoxel(@PathVariable voxelId: UUID) {
        LOGGER.info("[Empty Voxel] for user $voxelId")
        workerService.delete(voxelId)
    }

    companion object {
        private val LOGGER: Log = LogFactory.getLog(VoxelController::class.java)
    }
}
