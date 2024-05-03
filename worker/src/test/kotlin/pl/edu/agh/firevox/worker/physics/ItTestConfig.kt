package pl.edu.agh.firevox.worker.physics

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer

@Configuration
@PropertySource("classpath:application.yml")
class ItTestConfig {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @Bean
    fun postgreSQLContainer(): PostgreSQLContainer<*> {
        val postgresContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("firevox")
            .withUsername("firevox")
            .withPassword("firevox")
            .withFileSystemBind("/home/adrian/IdeaProjects/FireVox/worker/test_docker_out", "/home", BindMode.READ_WRITE)
        postgresContainer.setCommand(
            "postgres",
            "-c", "fsync=off",
            "-c", "shared_preload_libraries=pg_stat_statements",
//            "-c", "log_statement=all",
//            "-c", "work_mem=32MB"
        )
        postgresContainer.start()
//        log.debug(postgresContainer.logs); // prints startup logs
//        postgresContainer.followOutput(Slf4jLogConsumer(log))
        return postgresContainer
    }

    @Bean
    fun dataSourceConfig(postgreSQLContainer: PostgreSQLContainer<*>) = DataSourceConfig(
        postgreSQLContainer.jdbcUrl,
        postgreSQLContainer.username,
        postgreSQLContainer.password
    ).also { log.error(it.toString()) }

    @Bean
    @Primary
    fun getDataSource(dataSourceConfig: DataSourceConfig) = HikariDataSource(HikariConfig().also {
        it.driverClassName = "org.postgresql.Driver"
        it.jdbcUrl = dataSourceConfig.url
        it.username = dataSourceConfig.username
        it.password = dataSourceConfig.password
    })

}

data class DataSourceConfig(
    val url: String,
    val username: String,
    val password: String,
)
