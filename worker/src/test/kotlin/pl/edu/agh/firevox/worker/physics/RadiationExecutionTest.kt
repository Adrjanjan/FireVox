package pl.edu.agh.firevox.worker.physics

import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
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
import java.io.FileOutputStream
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.1",
        "firevox.voxel.size=0.01",
    ],
    classes = [WorkerApplication::class, ItTestConfig::class]
)
class RadiationExecutionTest(
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
) : ShouldSpec({

    context("calculate radiation test") {
        val simulationTimeInSeconds = 100 // * 60
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        // given
        val input = withContext(Dispatchers.IO) {
            getFile("radiation_test.vox")
        }
        // when
        val model = VoxFormatParser.read(input)

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

        val voxels = model.voxels.map { (k, _) ->
            Voxel(
                VoxelKey(k.x, k.y, k.z),
                evenIterationMaterial = baseMaterial,
                evenIterationTemperature = if(isBoundary(k)) 700.toKelvin() else 25.toKelvin(),
                oddIterationMaterial = baseMaterial,
                oddIterationTemperature = if(isBoundary(k)) 700.toKelvin() else 25.toKelvin(),
                isBoundaryCondition = false //isBoundary(k)
            )
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

        // then create planes
        val pointsToNormals = listOf(
            VoxelKey(0, 0, 1) to VoxelKey(1, 0, 0),
            VoxelKey(1, 99, 1) to VoxelKey(0, -1, 0),
            VoxelKey(99, 1, 1) to VoxelKey(-1, 0, 0),
            VoxelKey(1, 1, 99) to VoxelKey(0, 0, -1),

            VoxelKey(52, 1, 0) to VoxelKey(0, 0, 1),
            VoxelKey(32, 1, 0) to VoxelKey(0, 0, 1),

            VoxelKey(49, 10, 1) to VoxelKey(-1, 0, 0),
            VoxelKey(51, 10, 1) to VoxelKey(1, 0, 0),
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
                log.info("Finished calculations")
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
                FileOutputStream("radiation_result.vox")
            )
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

fun isBoundary(k: VoxelKey) = k.x in 52..98 && k.z == 0


fun ShouldSpec.getFile(name: String) = ClassPathResource(name).inputStream