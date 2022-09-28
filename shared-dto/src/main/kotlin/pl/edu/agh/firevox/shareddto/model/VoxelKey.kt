package pl.edu.agh.firevox.shareddto.model

class VoxelKey(val key: String) {
    constructor(
        x: Long,
        y: Long,
        z: Long,
    ) : this("$x/$y/$z")
}
