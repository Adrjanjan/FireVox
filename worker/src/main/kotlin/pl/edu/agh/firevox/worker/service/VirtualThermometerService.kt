package pl.edu.agh.firevox.worker.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.thermometer.VirtualThermometer
import pl.edu.agh.firevox.shared.model.thermometer.VirtualThermometerRepository
import pl.edu.agh.firevox.shared.model.toCelsius
import java.text.DecimalFormat

@Service
class VirtualThermometerService(
    val virtualThermometerRepository: VirtualThermometerRepository,
    val jdbcTemplate: JdbcTemplate
) {
    val format = DecimalFormat("#.#######")

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun create(key: VoxelKey): VirtualThermometer {
        log.info("Creating virtual thermometer for voxel $key")
        // do nothing :)
        return VirtualThermometer(voxelKey = key, measurement = 0.0)
    }

    fun delete(key: VoxelKey) {
        log.info("Creating virtual thermometer for voxel ")
        virtualThermometerRepository.deleteByVoxelKey(key)
    }

    @Transactional
    fun update(key: VoxelKey, value: Double) {
        virtualThermometerRepository.findLastIteration(key)?.let {
            virtualThermometerRepository.save(
                VirtualThermometer(voxelKey = key, measurement = value.toCelsius(), iteration = it + 1)
            )
        }
    }

    @Transactional
    fun getMeasurements(key: VoxelKey) =  virtualThermometerRepository.findMeasurements(key).joinToString(separator = "\n")

    fun updateDirectly(emitterThermometer: VoxelKey, i: Int) {
        jdbcTemplate.update("""
            insert into virtual_thermometer (iteration, measurement, x, y, z)
            select  
                $i,  
                case when MOD($i, 2) = 0 then v.even_iteration_temperature else v.odd_iteration_temperature end,
                ${emitterThermometer.x},
                ${emitterThermometer.y}, 
                ${emitterThermometer.z}
            from voxels v 
                where v.x = ${emitterThermometer.x} 
                and v.y = ${emitterThermometer.y} 
                and v.z = ${emitterThermometer.z}
        """.trimIndent()
        )
    }
}
