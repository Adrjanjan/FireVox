package pl.edu.agh.firevox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.integration.config.EnableIntegration


@SpringBootApplication
@EnableJpaRepositories
//@EnableIntegration
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox.*", "pl.edu.agh.firevox.shared.*"]
)
class FireVox

fun main(args: Array<String>) {
    runApplication<FireVox>(*args)
}
