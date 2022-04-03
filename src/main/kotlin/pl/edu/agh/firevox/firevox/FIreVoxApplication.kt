package pl.edu.agh.firevox.firevox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FIreVoxApplication

fun main(args: Array<String>) {
	runApplication<FIreVoxApplication>(*args)
}
