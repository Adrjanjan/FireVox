package pl.edu.agh.firevox.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import pl.edu.agh.firevox.shared.config.FireVoxProperties


@SpringBootApplication
@EnableJpaRepositories
@EnableConfigurationProperties(FireVoxProperties::class)
@ComponentScan(
    basePackages = ["pl.edu.agh.firevox.worker.*", "pl.edu.agh.firevox.shared.*"]
)
class VoxelApplication

fun main(args: Array<String>) {
    runApplication<VoxelApplication>(*args)
}
