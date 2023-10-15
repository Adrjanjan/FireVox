package pl.edu.agh.firevox.synchroniser

import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.SpringBeanJobFactory

@Configuration
class SchedulerConfiguration {
    inner class AutowireCapableBeanJobFactory(private val beanFactory: AutowireCapableBeanFactory) :
        SpringBeanJobFactory() {

        @Throws(Exception::class)
        override fun createJobInstance(bundle: TriggerFiredBundle): Any {
            val jobInstance = super.createJobInstance(bundle)
            beanFactory.autowireBean(jobInstance)
            beanFactory.initializeBean(jobInstance, jobInstance.toString())
            return jobInstance
        }
    }

    @Bean
    fun schedulerFactory(applicationContext: ApplicationContext): SchedulerFactoryBean {
        val schedulerFactoryBean = SchedulerFactoryBean()
        schedulerFactoryBean.setJobFactory(AutowireCapableBeanJobFactory(applicationContext.autowireCapableBeanFactory))
        return schedulerFactoryBean
    }

    @Bean
    @Throws(SchedulerException::class)
    fun scheduler(applicationContext: ApplicationContext): Scheduler {
        val scheduler: Scheduler = schedulerFactory(applicationContext).scheduler
        scheduler.start()
        return scheduler
    }
}