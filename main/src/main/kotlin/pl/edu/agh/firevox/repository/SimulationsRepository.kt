package pl.edu.agh.firevox.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.edu.agh.firevox.model.Simulation
import java.util.*

@Repository
interface SimulationsRepository : JpaRepository<Simulation, UUID>
