package pl.edu.agh.firevox.worker.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.worker.model.Voxel
import java.util.*

@Repository
interface VoxelRepository : CrudRepository<Voxel?, UUID>