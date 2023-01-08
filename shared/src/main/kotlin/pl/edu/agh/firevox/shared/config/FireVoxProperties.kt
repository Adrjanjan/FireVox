package pl.edu.agh.firevox.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("firevox")
data class FireVoxProperties (
    var maxSize: Int
)