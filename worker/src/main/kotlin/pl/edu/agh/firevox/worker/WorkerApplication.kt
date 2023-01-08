package pl.edu.agh.firevox.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan("pl.edu.agh.firevox")
@EnableJpaRepositories(basePackages = ["pl.edu.agh.firevox"])
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox", "pl.edu.agh.firevox.shared"]
)
class WorkerApplication

fun main(args: Array<String>) {
    runApplication<WorkerApplication>(*args)
}
