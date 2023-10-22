package pl.edu.agh.firevox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication
@EntityScan("pl.edu.agh.firevox")
@EnableJpaRepositories(basePackages = ["pl.edu.agh.firevox"])
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox.*", "pl.edu.agh.firevox.shared.*"]
)
@EnableTransactionManagement
class FireVox

fun main(args: Array<String>) {
    runApplication<FireVox>(*args)
}
