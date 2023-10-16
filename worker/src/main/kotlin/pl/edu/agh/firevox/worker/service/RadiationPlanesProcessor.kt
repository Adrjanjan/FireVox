package pl.edu.agh.firevox.worker.service
import org.slf4j.Logger

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import pl.edu.agh.firevox.shared.messaging.RadiationPlanesProcessingMessageSender
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlane
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneDto
import pl.edu.agh.firevox.worker.physics.RadiationCalculator
import java.util.function.Consumer

@Component
class RadiationPlanesProcessor(
    private val radiationCalculator: RadiationCalculator,
    private val radiationPlanesProcessingMessageSender: RadiationPlanesProcessingMessageSender,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    fun radiation(): Consumer<RadiationPlaneDto> {
        return Consumer<RadiationPlaneDto> { k ->
            log.info("Processing radiation plane ${k.radiationPlaneId} for iteration ${k.iteration}")
            if(!radiationCalculator.calculate(k.radiationPlaneId, k.iteration)) {
                radiationPlanesProcessingMessageSender.send(k)
            } else {
                radiationPlanesProcessingMessageSender.send(k.also { it.iteration += 1 })
            }
        }
    }

}

