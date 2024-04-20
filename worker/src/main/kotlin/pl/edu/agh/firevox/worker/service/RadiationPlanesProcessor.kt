package pl.edu.agh.firevox.worker.service
import org.slf4j.Logger

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import pl.edu.agh.firevox.shared.messaging.RadiationPlanesProcessingMessageSender
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneDto
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.worker.physics.RadiationCalculator
import java.util.function.Consumer

@Component
class RadiationPlanesProcessor(
    private val radiationCalculator: RadiationCalculator,
    private val countersRepository: CountersRepository,
    private val radiationPlanesProcessingMessageSender: RadiationPlanesProcessingMessageSender,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    fun radiation(): Consumer<RadiationPlaneDto> {
        return Consumer<RadiationPlaneDto> { k ->
            log.info("Processing radiation plane ${k.radiationPlaneId} for iteration ${k.iteration}")
            if(!radiationCalculator.calculateFetchingFromDb(k.radiationPlaneId, k.iteration)) {
                radiationPlanesProcessingMessageSender.send(k)
            } else {
                if(k.iteration.toLong() == countersRepository.findByIdOrNull(CounterId.MAX_ITERATIONS)?.count!!){
                    VoxelsProcessor.log.info("Finishing calculation for radiation plane ${k.radiationPlaneId} on iteration ${k.iteration}")
                }
                radiationPlanesProcessingMessageSender.send(k.also { it.iteration += 1 })
            }
        }
    }

}

