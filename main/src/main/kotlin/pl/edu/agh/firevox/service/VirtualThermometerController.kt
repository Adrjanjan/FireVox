package pl.edu.agh.firevox.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.thermometer.VirtualThermometerRepository

@RestController
@RequestMapping("thermometer")
class VirtualThermometerController(
    private val virtualThermometerRepository: VirtualThermometerRepository,
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @GetMapping
    fun getThermometerMeasurements(@RequestBody key: VoxelKey): ResponseEntity<ByteArray> {
        log.info("Exporting thermometer measurements for key $key")
        val it = virtualThermometerRepository.findByVoxelKey(key) ?: return ResponseEntity.noContent().build()
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_PLAIN
        headers.setContentDispositionFormData("attachment", "$key.csv".replace("/", "_"))
        return ResponseEntity(it.measurements.toByteArray(), headers, HttpStatus.OK)
    }

}