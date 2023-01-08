package pl.edu.agh.firevox.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding


@ConfigurationProperties("firevox")
class FireVoxProperties @ConstructorBinding constructor(
    var maxSize: Int
)