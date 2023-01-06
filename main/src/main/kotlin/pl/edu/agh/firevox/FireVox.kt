package pl.edu.agh.firevox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.integration.config.EnableIntegration
import pl.edu.agh.firevox.shared.config.FireVoxProperties


@SpringBootApplication
@EnableJpaRepositories
@EnableIntegration
@EnableConfigurationProperties(FireVoxProperties::class)
class FireVox

fun main(args: Array<String>) {
    runApplication<FireVox>(*args)
}
