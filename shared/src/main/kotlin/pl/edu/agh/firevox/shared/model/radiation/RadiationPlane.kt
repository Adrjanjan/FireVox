package pl.edu.agh.firevox.shared.model.radiation

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.model.VoxelKey
import kotlin.jvm.Transient


@Entity
class RadiationPlane(
    // after it was calculated we do not need the points and normal vector
    @Transient
    val a: VoxelKey,
    @Transient
    val b: VoxelKey,
    @Transient
    val c: VoxelKey,
    @Transient
    val d: VoxelKey,

    // no longer transient so it can be used to detection during flame creation
    val normalVector: VoxelKey,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @OneToMany(mappedBy = "parent", cascade = [CascadeType.ALL])
    val childPlanes: MutableList<PlanesConnection> = mutableListOf(),

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "plane_voxels",
        joinColumns = [JoinColumn(name = "plane_id", referencedColumnName = "id")],
        indexes = [Index(name = "plane_voxels_id", columnList = "plane_id")]
    )
    @AttributeOverrides(
        AttributeOverride(name = "x", column = Column(name = "voxel_key_x")),
        AttributeOverride(name = "y", column = Column(name = "voxel_key_y")),
        AttributeOverride(name = "z", column = Column(name = "voxel_key_z"))
    )
    val voxels: MutableSet<VoxelKey> = mutableSetOf(),

    val voxelsCount: Int,

    val area: Double,

    @Transient
    val fullPlane: List<VoxelKey> = listOf(),
) {
    @Transient
    val middle = VoxelKey((a.x + b.x + c.x + d.x) / 4, (a.y + b.y + c.y + d.y) / 4, (a.z + b.z + c.z + d.z) / 4)
    override fun toString(): String {
        return "RadiationPlane(id=$id, middle=$middle)"
    }

}

@Entity
@Table(name = "planes_connections")
class PlanesConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    val parent: RadiationPlane,

    @ManyToOne(fetch = FetchType.LAZY)
    val child: RadiationPlane,

    val viewFactor: Double,

    val parentVoxelsCount: Int,

    val childVoxelsCount: Int,

) {
    var qNet: Double = 0.0

    override fun toString(): String {
        return "PlanesConnection(id=$id, parent=$parent, child=$child)"
    }
}

@Repository
interface RadiationPlaneRepository : JpaRepository<RadiationPlane, Int> {


    @Query(
        """
            select case when MOD(:iteration, 2) = 0 
                then avg(v.evenIterationTemperature) 
                else avg(v.oddIterationTemperature) end 
            from Voxel v 
            where v.key IN (select p.voxels from RadiationPlane p where p.id = :id)
        """
    )
    fun planeAverageTemperature(id: Int, iteration: Int): Double

    @Query(
        """
            select p.id from RadiationPlane p 
            where :minimalAvgTemperature > (
                select avg(v.evenIterationTemperature) from Voxel v where v.key member of p.voxels
            )
        """
    )
    fun findStartingPlanes(minimalAvgTemperature: Double): List<Int>

}

data class RadiationPlaneDto(
    val radiationPlaneId: Int,
    var iteration: Int,
)