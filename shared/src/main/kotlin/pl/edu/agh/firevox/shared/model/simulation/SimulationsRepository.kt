package pl.edu.agh.firevox.shared.model.simulation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SimulationsRepository : JpaRepository<Simulation, UUID> {

    @Query("select s.sizeX, s.sizeY, s.sizeZ from Simulation s limit 1")
    fun fetchSize(): SimulationSizeView

}
