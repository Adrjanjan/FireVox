package pl.edu.agh.firevox.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class VoxelApplication

fun main(args: Array<String>) {
    runApplication<VoxelApplication>(*args)
}
