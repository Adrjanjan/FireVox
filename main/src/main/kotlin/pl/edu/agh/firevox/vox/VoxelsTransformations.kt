package pl.edu.agh.firevox.vox

import pl.edu.agh.firevox.vox.chunks.Rotation
import pl.edu.agh.firevox.vox.chunks.Translation
import kotlin.math.absoluteValue

object VoxelsTransformations {

    fun MutableMap<VoxelKey, VoxelMaterialId>.translate(t: Translation?) = if (t == null) this else
        this.mapKeys {
            val key = it.key
            key.copy(x = key.x + t.x, y = key.y + t.y, z = key.z + t.z)
        }.toMutableMap()

    fun MutableMap<VoxelKey, VoxelMaterialId>.rotateVoxelsAndMoveToPositiveCoords(r: Rotation?): MutableMap<VoxelKey, VoxelMaterialId> =
        if (r == null) this else
            this.mapKeys {
                it.key.run {
                    val rx = r.rot[0][0] * x + r.rot[0][1] * y + r.rot[0][2] * z
                    val ry = r.rot[1][0] * x + r.rot[1][1] * y + r.rot[1][2] * z
                    val rz = r.rot[2][0] * x + r.rot[2][1] * y + r.rot[2][2] * z
                    VoxelKey(rx, ry, rz)
                }
            }.toMutableMap().let { map ->
                map.translate(
                    Translation(
                        map.keys.minOf { it.x }.absoluteValue,
                        map.keys.minOf { it.y }.absoluteValue,
                        map.keys.minOf { it.z }.absoluteValue,
                    )
                )
            }

    fun MutableMap<VoxelKey, VoxelMaterialId>.sizeInDimension(dimensionFunction: (VoxelKey) -> Int) =
        this.keys.toList().maxOf(dimensionFunction) - this.keys.toList().minOf(dimensionFunction)  // +- 1 ??

}

