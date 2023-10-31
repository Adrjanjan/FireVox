package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.shared.model.simulation.counters.Counter
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import java.io.FileOutputStream
import kotlin.math.roundToInt

import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.worker.service.VirtualThermometerService

@SpringBootTest(
    properties = ["firevox.timestep=0.1", "firevox.voxel.size=0.01", "firevox.smokeIntoFireThreshold: 393.15"],
    classes = [WorkerApplication::class]
)
class TwoVoxelsConductionVerificationTest(
    val calculationService: CalculationService,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    private val virtualThermometerService: VirtualThermometerService,
) : ShouldSpec({

    context("save voxels from file") {
        val simulationTimeInSeconds = 10 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))


        val baseMaterial = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 2700.0,
            baseTemperature = 20.toKelvin(),
            thermalConductivityCoefficient = 235.0,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 897.0,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmission = null,
            deformationTemperature = 1000.0.toKelvin(),
        ).also(physicalMaterialRepository::save)

        var voxels = mutableListOf<Voxel>(
            Voxel(
                VoxelKey(1, 0, 1),
                evenIterationMaterial = baseMaterial,
                evenIterationTemperature = 20.toKelvin(),
                oddIterationMaterial = baseMaterial,
                oddIterationTemperature = 20.0.toKelvin()
            )
        )
        voxels.add(
            Voxel(
                VoxelKey(1, 1, 1),
                evenIterationMaterial = baseMaterial,
                evenIterationTemperature = 100.toKelvin(),
                oddIterationMaterial = baseMaterial,
                oddIterationTemperature = 100.toKelvin()
            )
        )
//        voxels.add(
//            Voxel(
//                VoxelKey(1, 2, 1),
//                evenIterationMaterial = baseMaterial,
//                evenIterationTemperature = 100.toKelvin(),
//                oddIterationMaterial = baseMaterial,
//                oddIterationTemperature = 100.toKelvin()
//            )
//        )
        virtualThermometerService.create(voxels[0].key)
        virtualThermometerService.create(voxels[1].key)
//        virtualThermometerService.create(voxels[2].key)

        // set boundary conditions
//        voxels.filter { it.key.x == 0 }
//            .forEach {
//                it.evenIterationTemperature = 300.toKelvin()
//                it.oddIterationTemperature = 300.toKelvin()
//                it.isBoundaryCondition = true
//            }
//        voxels.filter { it.key.x == 30 * scale - 1 }
//            .forEach { it.isBoundaryCondition = true }

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Conduction test",
                parentModel = SingleModel(name = "Parent model"),
                sizeX = sizeX,
                sizeY = sizeY,
                sizeZ = sizeZ,
            )
        )
        voxelRepository.saveAll(voxels)
        voxelRepository.flush()

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
//                voxels.parallelStream().forEach { v ->
                voxels.forEach { v ->
                    calculationService.calculate(v.key, i)
                }
                countersRepository.increment(CounterId.CURRENT_ITERATION)
            }

            val result = voxelRepository.findAll()
//            result.forEach { log.info(it.toString()) }
            val min = result.minOf { it.evenIterationTemperature }
            val max = result.maxOf { it.evenIterationTemperature }
            log.info("End of the processing, starting to write result, max temp: ${max.toCelsius()}, min temp: ${min.toCelsius()}")

            VoxFormatParser.write(
                result.associate {
                    it.key to VoxFormatParser.toPaletteLinear(
                        value = it.evenIterationTemperature,
                        min = min,
                        max = max
                    )
                },
                Palette.temperaturePalette,
                sizeX,
                sizeY,
                sizeZ,
                FileOutputStream("2blocks.vox")
            )
            log.info(virtualThermometerService.getMeasurements(voxels[0].key))
            log.info(virtualThermometerService.getMeasurements(voxels[1].key))
//            log.info(virtualThermometerService.getMeasurements(voxels[2].key))
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}