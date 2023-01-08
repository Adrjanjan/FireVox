package pl.edu.agh.firevox.service

import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository

@Service
class SimulationCreationService(
    private val voxelRepository: CustomVoxelRepository
) {
}