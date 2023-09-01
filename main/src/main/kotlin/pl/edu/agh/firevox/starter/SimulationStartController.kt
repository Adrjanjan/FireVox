package pl.edu.agh.firevox.starter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.service.SimulationCreationService
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@RestController
@RequestMapping("start")
class SimulationStartController(
    private val simulationCreationService: SimulationCreationService,
    @Value("firevox.simulation.files.path")
    private val simulationFilesPath: String
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping
    fun startSimulation(@RequestBody model: ModelDescriptionDto) {
        log.info("Starting simulation for file ${model.outputName}")
        simulationCreationService.start(model)
    }

    @PostMapping("/model", consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun addModelFiles(@RequestBody zip: ByteArray): ResponseEntity<String> = try {
        val zipInputStream = ZipInputStream(ByteArrayInputStream(zip))

        val outputDir = Path(simulationFilesPath).toFile()
        outputDir.mkdirs()

        var entry: ZipEntry?
        while (zipInputStream.nextEntry.also { entry = it } != null) {
            val outputFile = File(outputDir, entry!!.name)
            outputFile.outputStream().use { output ->
                zipInputStream.copyTo(output)
            }
        }
        ResponseEntity.ok("Files unzipped successfully.")
    } catch (e: Exception) {
        ResponseEntity.badRequest().body("Error unzipping files: ${e.message}")
    }

    @OptIn(ExperimentalPathApi::class)
    @DeleteMapping("/model")
    fun clearModel(): ResponseEntity<Unit> {
        Path(simulationFilesPath).deleteRecursively()
        return ResponseEntity.ok().build()
    }
}