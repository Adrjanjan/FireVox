package pl.edu.agh.firevox.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EntityScan("pl.edu.agh.firevox.shared")
@EnableJpaRepositories(basePackages = ["pl.edu.agh.firevox.shared"])
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox", "pl.edu.agh.firevox.shared"]
)
@PropertySource("classpath:application.yml")
@EnableTransactionManagement
class WorkerApplication

fun main(args: Array<String>) {
    runApplication<WorkerApplication>(*args)
}
