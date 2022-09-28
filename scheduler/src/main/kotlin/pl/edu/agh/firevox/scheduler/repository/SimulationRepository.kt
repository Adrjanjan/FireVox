package pl.edu.agh.firevox.scheduler.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.scheduler.model.Simulation
import java.util.*

@Repository
interface SimulationRepository : JpaRepository<Simulation, UUID>
