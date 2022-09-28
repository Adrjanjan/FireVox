package pl.edu.agh.firevox.scheduler.service

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.scheduler.repository.SimulationRepository

@Service
class SchedulerService {

    @Autowired
    lateinit var simulationRepository: SimulationRepository


    companion object {
        private val LOGGER: Log = LogFactory.getLog(SchedulerService::class.java)
    }


}
