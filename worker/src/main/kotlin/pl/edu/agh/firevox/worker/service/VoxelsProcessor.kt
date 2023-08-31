package pl.edu.agh.firevox.worker.service
import org.slf4j.Logger

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration
import java.util.function.Consumer

@Component
class VoxelsProcessor(
    private val calculationService: CalculationService
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    fun input(): Consumer<VoxelKeyIteration> {
        return Consumer<VoxelKeyIteration> { k ->
            log.info("Processing key ${k.key} for iteration ${k.iteration}")
            calculationService.calculate(k.key, k.iteration)
        }
    }

}