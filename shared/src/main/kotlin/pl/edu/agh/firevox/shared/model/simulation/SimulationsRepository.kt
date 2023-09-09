package pl.edu.agh.firevox.shared.model.simulation

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SimulationsRepository : JpaRepository<Simulation, UUID> {

    @Query("select new pl.edu.agh.firevox.shared.model.simulation.SimulationSizeView(s.sizeX, s.sizeY, s.sizeZ) from Simulation s")
    fun fetchSize(): SimulationSizeView

}
