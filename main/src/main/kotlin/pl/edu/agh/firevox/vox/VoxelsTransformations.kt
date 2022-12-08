package pl.edu.agh.firevox.vox

import pl.edu.agh.firevox.vox.chunks.Rotation
import pl.edu.agh.firevox.vox.chunks.Translation

object VoxelsTransformations {

    fun MutableList<Voxel>.translate(t: Translation?) = if (t == null) this else
        this.map { it.copy(x = it.x + t.x, y = it.y + t.y, z = it.z + t.z) }.toMutableList()

    fun MutableList<Voxel>.rotateVoxels(r: Rotation?) = if (r == null) this else
        this.map {
            val sizeX = this.sizeInDimension { v -> v.x }
            val sizeY = this.sizeInDimension { v -> v.y }
            val sizeZ = this.sizeInDimension { v -> v.z }
            val fx = it.x - sizeX / 2f
            val fy = it.y - sizeY / 2f
            val fz = it.z - sizeZ / 2f
            val rx: Int = (r.rot[0][0] * fx + r.rot[0][1] * fy + r.rot[0][2] * fz).toInt()
            val ry: Int = (r.rot[1][0] * fx + r.rot[1][1] * fy + r.rot[1][2] * fz).toInt()
            val rz: Int = (r.rot[2][0] * fx + r.rot[2][1] * fy + r.rot[2][2] * fz).toInt()
            Voxel(rx, ry, rz, it.colorIndex)
        }.toMutableList()

    fun MutableList<Voxel>.sizeInDimension(dimensionFunction: (Voxel) -> Int) =
        this.maxOf(dimensionFunction) - this.minOf(dimensionFunction)  // +- 1 ??

}

