package pl.edu.agh.firevox.worker.service
import org.slf4j.Logger

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import pl.edu.agh.firevox.shared.messaging.VoxelProcessingMessageSender
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import java.util.function.Consumer

@Component
class VoxelsProcessor(
    private val calculationService: CalculationService,
    private val countersRepository: CountersRepository,
    private val voxelProcessingMessageSender: VoxelProcessingMessageSender,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    fun voxels(): Consumer<VoxelKeyIteration> {
        return Consumer<VoxelKeyIteration> { k ->
            log.info("Processing key ${k.key} for iteration ${k.iteration}")
            if(!calculationService.calculate(k.key, k.iteration)) {
                voxelProcessingMessageSender.send(k)
            } else {
                if(k.iteration.toLong() == countersRepository.findByIdOrNull(CounterId.MAX_ITERATIONS)?.count!!){
                    log.info("Finishing calculation for voxel ${k.key} on iteration ${k.iteration}")
                }
                voxelProcessingMessageSender.send(k.key, k.iteration + 1)
            }
        }
    }

}