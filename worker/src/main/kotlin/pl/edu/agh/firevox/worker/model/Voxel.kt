package pl.edu.agh.firevox.worker.model

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import pl.edu.agh.firevox.shareddto.model.VoxelKey
import java.util.*

@RedisHash("Voxel", timeToLive = 172800) // 48h
class Voxel(
    @Id
    val voxelId: VoxelKey,
    val x: Long,
    val y: Long,
    val z: Long,

    val energy: Double,
    val temperature: Double,

    )