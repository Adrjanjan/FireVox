package pl.edu.agh.firevox.shareddto.model

class VoxelKey(val x: Long, val y: Long, val z: Long) {
    override fun toString() =  "$x/$y/$z"
}
