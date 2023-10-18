package pl.edu.agh.firevox.worker.physics

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import org.testcontainers.containers.PostgreSQLContainer

@Configuration
@PropertySource("classpath:application.yml")
class ItTestConfig {
    @Bean
    fun postgreSQLContainer(): PostgreSQLContainer<*> {
        val postgresContainer = PostgreSQLContainer("postgres:latest")
            .withDatabaseName("firevox")
            .withUsername("firevox")
            .withPassword("firevox")
        postgresContainer.start()
        return postgresContainer
    }

    @Bean
    fun dataSourceConfig(postgreSQLContainer: PostgreSQLContainer<*>) = DataSourceConfig(
        postgreSQLContainer.jdbcUrl,
        postgreSQLContainer.username,
        postgreSQLContainer.password
    )

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
