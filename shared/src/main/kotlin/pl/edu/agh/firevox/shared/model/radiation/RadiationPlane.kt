package pl.edu.agh.firevox.shared.model.radiation

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.shared.model.Voxel
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
    @Transient
    val normalVector: VoxelKey,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null,

    @OneToMany(mappedBy = "parentPlane", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val childPlanes: MutableList<PlanesConnection> = mutableListOf(),

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "plane_voxels",
        joinColumns = [JoinColumn(name = "plane_id", referencedColumnName = "id")],
        inverseJoinColumns = [
            JoinColumn(name = "voxel_key_x", referencedColumnName = "x"),
            JoinColumn(name = "voxel_key_y", referencedColumnName = "y"),
            JoinColumn(name = "voxel_key_z", referencedColumnName = "z"),
        ]
    )
    val voxels: MutableSet<Voxel> = mutableSetOf(),

    val area: Double,
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val parentPlane: RadiationPlane,

    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    val child: RadiationPlane,

    val viewFactor: Double

) {
    var qNet: Double = 0.0
}

@Repository
interface RadiationPlaneRepository : JpaRepository<RadiationPlane, Int>{

    @Query(
        """
            select rp from RadiationPlane rp join PlanesConnection pc where pc.qNet > 0
        """
    )
    fun findWithPositiveQNets(): List<RadiationPlane>


    @Query(
        """
            select update_temperatures(:iteration, :volume)
        """, nativeQuery = true
    )
    @Modifying
    fun updateTemperatures(iteration: Long, volume: Double)


    @Query(
        """
            select p.id from RadiationPlane p 
            where :minimalAvgTemperature > (select avg(v.evenIterationTemperature) from Voxel v where v member of p.voxels)
        """
    )
    fun findStartingPlanes(minimalAvgTemperature: Double): List<Int>
}

data class RadiationPlaneDto(
    val radiationPlaneId: Int,
    var iteration: Int,
)