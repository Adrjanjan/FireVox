package pl.edu.agh.firevox.synchroniser

import org.quartz.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SynchronisationJobManager(
    private val scheduler: Scheduler,
    @Value("\${firevox.synchronisation.time}") val synchronisationTime: Long = 100,
) {

    private val jobName = "iteration-synchroniser"
    private val key = JobKey.jobKey(jobName)

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun scheduleJob() {
        val job = newSynchronisationJob()
        scheduler.scheduleJob(job, trigger(job)).also { log.info("$it") }
    }

    fun pause() {
        scheduler.pauseJob(key)
    }

    fun resume() {
        scheduler.resumeJob(key)
    }

    fun newSynchronisationJob(): JobDetail {
        return JobBuilder.newJob().ofType(IterationSynchroniser::class.java).storeDurably()
            .withIdentity(key)
            .withDescription("synchronisation")
            .build()
    }

    private fun trigger(jobDetail: JobDetail): SimpleTrigger {
        return TriggerBuilder.newTrigger().forJob(jobDetail)
            .withIdentity(jobDetail.key.name, jobDetail.key.group)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(synchronisationTime))
            .build()
    }

}