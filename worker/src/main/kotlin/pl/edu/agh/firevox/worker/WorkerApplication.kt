package pl.edu.agh.firevox.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories


@SpringBootApplication
@EnableJpaRepositories
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox.*", "pl.edu.agh.firevox.shared.*"]
)
class WorkerApplication

fun main(args: Array<String>) {
    runApplication<WorkerApplication>(*args)
}
