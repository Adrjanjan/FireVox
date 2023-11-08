package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import pl.edu.agh.firevox.shared.model.*
import pl.edu.agh.firevox.shared.model.radiation.PlaneFinder
import pl.edu.agh.firevox.shared.model.radiation.RadiationPlaneRepository
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.shared.model.simulation.counters.Counter
import pl.edu.agh.firevox.shared.model.simulation.counters.CounterId
import pl.edu.agh.firevox.shared.model.simulation.counters.CountersRepository
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.shared.synchroniser.SynchroniserImpl
import pl.edu.agh.firevox.worker.WorkerApplication
import pl.edu.agh.firevox.worker.service.CalculationService
import pl.edu.agh.firevox.worker.service.VirtualThermometerService
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.1",
        "firevox.voxel.size=0.01",
        "firevox.smokeIntoFireThreshold=150"
    ],
    classes = [WorkerApplication::class, ItTestConfig::class]
)
class RadiationSimpleExecutionTest(
    val calculationService: CalculationService,
    val radiationCalculator: RadiationCalculator,
    val voxelRepository: VoxelRepository,
    val physicalMaterialRepository: PhysicalMaterialRepository,
    val simulationsRepository: SimulationsRepository,
    val countersRepository: CountersRepository,
    val radiationPlaneRepository: RadiationPlaneRepository,
    @Value("\${firevox.timestep}") val timeStep: Double,
    val planeFinder: PlaneFinder,
    val synchroniserImpl: SynchroniserImpl,
    val virtualThermometerService: VirtualThermometerService,
) : ShouldSpec({

    context("simple calculate parallel radiation test") {
        val simulationTimeInSeconds = 350 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        // given
        val baseMaterial = PhysicalMaterial(
            VoxelMaterial.CONCRETE,
            density = 2392.0,
            baseTemperature = 25.toKelvin(),
            thermalConductivityCoefficient = 2.071,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 936.3,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
        ).also(physicalMaterialRepository::save)

        val voxels = listOf(0, 11).flatMap { x ->
            (0..9).flatMap { y ->
                (0..9).map { z ->
                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = baseMaterial,
                        evenIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                        oddIterationMaterial = baseMaterial,
                        oddIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                        isBoundaryCondition = false //isBoundary(k)
                    )
                }
            }
        }

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Radiation test",
                parentModel = SingleModel(name = "Parent model"),
                sizeX = sizeX,
                sizeY = sizeY,
                sizeZ = sizeZ,
            )
        )
        voxelRepository.saveAll(voxels)
        voxelRepository.flush()

        val firstThermometerKey = VoxelKey(0, 1, 1)
        val secondThermometerKey = VoxelKey(11, 1, 1)
        virtualThermometerService.create(firstThermometerKey)
        virtualThermometerService.create(secondThermometerKey)

        // then create planes
        val pointsToNormals = listOf(
            firstThermometerKey to VoxelKey(1, 0, 0),
            secondThermometerKey to VoxelKey(-1, 0, 0),
        )

        val matrix = Array(sizeX) { _ ->
            Array(sizeY) { _ ->
                IntArray(sizeZ) { _ -> 0 }
            }
        }

        voxels.forEach {
            matrix[it.key.x][it.key.y][it.key.z] = VoxelMaterial.CONCRETE.colorId
        }

        var planes = planeFinder.findPlanes(matrix, pointsToNormals)
            .also {
                countersRepository.set(
                    CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
                    it.size.toLong()
                )
            }
        planes = planes.let(radiationPlaneRepository::saveAll)
        radiationPlaneRepository.flush()

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                voxels.parallelStream().forEach { v -> calculationService.calculate(v.key, i) }
                log.info("Finished conduction")
                planes.parallelStream().forEach { k -> radiationCalculator.calculate(k, i) }
                log.info("Finished radiation")
                synchroniserImpl.synchroniseRadiationResults(i.toLong())
                log.info("Finished synchronisation")
                countersRepository.increment(CounterId.CURRENT_ITERATION)
                log.info("Finished increment")
            }

            val result = voxelRepository.findAll()
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
                FileOutputStream("simple_radiation_result_parallel.vox")
            )

            withContext(Dispatchers.IO) {
                FileOutputStream("hotter_parallel.csv").use {
                    val v = virtualThermometerService.getMeasurements(firstThermometerKey).also(log::info)
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }

                FileOutputStream("cooler_parallel.csv").use {
                    val v = virtualThermometerService.getMeasurements(secondThermometerKey).also(log::info)
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }
            }
        }
    }

    context("simple calculate perpendicular radiation test") {
        val simulationTimeInSeconds = 350 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        // given
        val baseMaterial = PhysicalMaterial(
            VoxelMaterial.CONCRETE,
            density = 2392.0,
            baseTemperature = 25.toKelvin(),
            thermalConductivityCoefficient = 2.071,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 936.3,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
        ).also(physicalMaterialRepository::save)

        val voxels = listOf(0).flatMap { x ->
            (1..10).flatMap { y ->
                (1..10).map { z ->
                    Voxel(
                        VoxelKey(x, y, z),
                        evenIterationMaterial = baseMaterial,
                        evenIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                        oddIterationMaterial = baseMaterial,
                        oddIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                        isBoundaryCondition = false //isBoundary(k)
                    )
                }
            }
        }.toMutableList()

        voxels.addAll(
            (1..10).flatMap { x ->
                (1..10).flatMap { y ->
                    listOf(0).map { z ->
                        Voxel(
                            VoxelKey(x, y, z),
                            evenIterationMaterial = baseMaterial,
                            evenIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                            oddIterationMaterial = baseMaterial,
                            oddIterationTemperature = if (x == 0) 700.toKelvin() else 25.toKelvin(),
                            isBoundaryCondition = false //isBoundary(k)
                        )
                    }
                }
            }
        )

        val sizeX = voxels.maxOf { it.key.x } + 1
        val sizeY = voxels.maxOf { it.key.y } + 1
        val sizeZ = voxels.maxOf { it.key.z } + 1

        simulationsRepository.save(
            Simulation(
                name = "Radiation test",
                parentModel = SingleModel(name = "Parent model"),
                sizeX = sizeX,
                sizeY = sizeY,
                sizeZ = sizeZ,
            )
        )
        voxelRepository.saveAll(voxels)
        voxelRepository.flush()

        val firstThermometerKey = VoxelKey(0, 1, 1)
        val secondThermometerKey = VoxelKey(1, 1, 0)
        virtualThermometerService.create(firstThermometerKey)
        virtualThermometerService.create(secondThermometerKey)

        // then create planes
        val pointsToNormals = listOf(
            firstThermometerKey to VoxelKey(1, 0, 0),
            secondThermometerKey to VoxelKey(0, 0, 1),
        )

        val matrix = Array(sizeX) { _ ->
            Array(sizeY) { _ ->
                IntArray(sizeZ) { _ -> 0 }
            }
        }

        voxels.forEach {
            matrix[it.key.x][it.key.y][it.key.z] = VoxelMaterial.CONCRETE.colorId
        }

        var planes = planeFinder.findPlanes(matrix, pointsToNormals)
            .also {
                countersRepository.set(
                    CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
                    it.size.toLong()
                )
            }
        planes = planes.let(radiationPlaneRepository::saveAll)
        radiationPlaneRepository.flush()

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                voxels.parallelStream().forEach { v -> calculationService.calculateGivenVoxel(v, i) }
                log.info("Finished conduction")
                planes.parallelStream().forEach { k -> radiationCalculator.calculate(k, i) }
                log.info("Finished radiation")
                synchroniserImpl.synchroniseRadiationResults(i.toLong())
                log.info("Finished synchronisation")
                countersRepository.increment(CounterId.CURRENT_ITERATION)
                log.info("Finished increment")
            }

            val result = voxelRepository.findAll()
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
                FileOutputStream("simple_radiation_result_perpendicular.vox")
            )

            withContext(Dispatchers.IO) {
                FileOutputStream("hotter_perpendicular.csv").use {
                    val v = virtualThermometerService.getMeasurements(firstThermometerKey).also(log::info)
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }

                FileOutputStream("cooler_perpendicular.csv").use {
                    val v = virtualThermometerService.getMeasurements(secondThermometerKey).also(log::info)
                    OutputStreamWriter(it).use { outputStreamWriter ->
                        outputStreamWriter.write(v)
                    }
                }
            }
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}