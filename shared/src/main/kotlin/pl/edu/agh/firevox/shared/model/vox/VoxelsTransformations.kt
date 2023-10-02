package pl.edu.agh.firevox.shared.model.vox

import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.vox.chunks.Rotation
import pl.edu.agh.firevox.shared.model.vox.chunks.Translation

object VoxelsTransformations {

    fun MutableMap<VoxelKey, VoxelMaterialId>.translate(t: Translation?) = if (t == null) this else this.mapKeys {
        val key = it.key
        key.copy(x = key.x + t.x, y = key.y + t.y, z = key.z + t.z)
    }.toMutableMap()

    fun MutableMap<VoxelKey, VoxelMaterialId>.rotateVoxels(r: Rotation?): MutableMap<VoxelKey, VoxelMaterialId> =
        if (r == null) this
        // https://computergraphics.stackexchange.com/questions/5232/row-and-column-majored-rotation-matrix-pre-or-post-multiplied
        // val Px = x∗rx + y∗ux + z∗fx
        // val Py = x∗ry + y∗uy + z∗fy
        // val Pz = x∗rz + y∗uz + z∗fz

        else this.mapKeys {
            val newX = r.rot[0][0] * it.key.x + r.rot[0][1] * it.key.y + r.rot[0][2] * it.key.z
            val newY = r.rot[1][0] * it.key.x + r.rot[1][1] * it.key.y + r.rot[1][2] * it.key.z
            val newZ = r.rot[2][0] * it.key.x + r.rot[2][1] * it.key.y + r.rot[2][2] * it.key.z
            it.key.x = newX
            it.key.y = newY
            it.key.z = newZ

            it.key
        }.toMutableMap()

    // +1 because indexes are from 0 to n, so size is n+1
    fun MutableMap<VoxelKey, VoxelMaterialId>.sizeInDimension(dimensionFunction: (VoxelKey) -> Int) =
        this.keys.maxOf(dimensionFunction) + 1


}

