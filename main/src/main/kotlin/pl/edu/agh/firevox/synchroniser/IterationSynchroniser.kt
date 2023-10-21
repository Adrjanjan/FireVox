package pl.edu.agh.firevox.synchroniser

import org.quartz.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.shared.synchroniser.SynchroniserImpl

@Component
@DisallowConcurrentExecution
class IterationSynchroniser(
    private val countersRepository: CountersRepository,
    private val synchroniserImpl: SynchroniserImpl,
    private val synchronisationJobManager: SynchronisationJobManager,
) : InterruptableJob {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Transactional
    override fun execute(jobExecutionContext: JobExecutionContext) {
        val iteration = countersRepository.findByIdOrNull(CounterId.CURRENT_ITERATION)?.count!!
        SynchroniserImpl.log.info("Running synchronisation $iteration")
        synchroniserImpl.verifyIterationFinish(iteration)
        synchroniserImpl.synchroniseRadiationResults(iteration)
        synchroniserImpl.resetCounters(iteration)
        verifyJobFinish(iteration)
    }

    private fun verifyJobFinish(iteration: Long) {
        if (iteration == countersRepository.findByIdOrNull(CounterId.MAX_ITERATIONS)?.count!!) {
            log.info("Finishing simulation on iteration $iteration")
            synchronisationJobManager.pause()
            return
        }
    }

    override fun interrupt() {
        log.info("Stopped synchronisation job")
    }

}
