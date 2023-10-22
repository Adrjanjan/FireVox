package pl.edu.agh.firevox.shared.synchroniser

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource


@Configuration
class JdbcTemplateConfig {
    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }
}