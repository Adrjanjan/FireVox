package pl.edu.agh.firevox.shared.model

import java.io.Serial
import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Embeddable
data class VoxelKey(
    @Column(name = "x")
    var x: Int,
    @Column(name = "y")
    var y: Int,
    @Column(name = "z")
    var z: Int
) : Serializable {
    override fun toString() = "$x/$y/$z"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VoxelKey

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int = Objects.hash(x, y, z)

    companion object {
        @Serial
        private const val serialVersionUID: Long = 1865432426569155870L
    }

    fun isBelow(other: VoxelKey) = this.x == other.x && this.y == other.y && this.z == other.z - 1

    fun isAbove(other: VoxelKey) = this.x == other.x && this.y == other.y && this.z == other.z + 1
    fun between(xRange: IntRange, yRange: IntRange, zRange: IntRange) = xRange.contains(x)
            && yRange.contains(y)
            && zRange.contains(z)

    operator fun minus(other: VoxelKey) =  VoxelKey(this.x - other.x, this.y - other.y, this.z - other.z)
}

data class VoxelKeyIteration(
    val key: VoxelKey,
    val iteration: Int
)