package pl.edu.agh.firevox.shared.model

data class VoxelKey(val x: Int, val y: Int, val z: Int) {
    override fun toString() =  "$x/$y/$z"
}
