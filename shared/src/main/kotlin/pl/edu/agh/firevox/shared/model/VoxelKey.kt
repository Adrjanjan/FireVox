package pl.edu.agh.firevox.shared.model

import java.io.Serial
import java.io.Serializable
import java.util.*
import jakarta.persistence.*

@Embeddable
data class VoxelKey(
    @Column(name="x")
    val x: Int,
    @Column(name="y")
    val y: Int,
    @Column(name="z")
    val z: Int
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
}
