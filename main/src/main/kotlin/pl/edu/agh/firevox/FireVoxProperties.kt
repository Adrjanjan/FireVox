package pl.edu.agh.firevox

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("firevox")
class FireVoxProperties (
    var maxSize: Int
)