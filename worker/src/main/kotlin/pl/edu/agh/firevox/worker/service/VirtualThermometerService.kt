package pl.edu.agh.firevox.worker.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.thermometer.VirtualThermometer
import pl.edu.agh.firevox.shared.model.thermometer.VirtualThermometerRepository
import pl.edu.agh.firevox.shared.model.toCelsius

@Service
class VirtualThermometerService(
    val virtualThermometerRepository: VirtualThermometerRepository
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun create(key: VoxelKey): VirtualThermometer {
        log.info("Creating virtual thermometer for voxel ")
        return virtualThermometerRepository.save(VirtualThermometer(voxelKey = key))
    }

    fun delete(key: VoxelKey) {
        log.info("Creating virtual thermometer for voxel ")
        virtualThermometerRepository.deleteByVoxelKey(key)
    }

    fun check(key: VoxelKey) = virtualThermometerRepository.existsByVoxelKey(key)

    @Transactional
    fun update(key: VoxelKey, value: Double) {
        virtualThermometerRepository.findByVoxelKey(key)?.let {
            it.measurements += ", ${value.toCelsius()}"
        }
    }

    @Transactional
    fun getMeasurements(key: VoxelKey) =
        virtualThermometerRepository.findByVoxelKey(key)?.measurements?.substring(2) ?: ""
}
