package pl.edu.agh.firevox.worker.physics

import com.google.common.math.IntMath.pow
import io.kotest.core.spec.style.ShouldSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
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
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.roundToInt

@SpringBootTest(
    properties = [
        "firevox.timestep=0.1",
        "firevox.voxel.size=0.01",
        "firevox.plane.size=200",
        "firevox.voxel.ambient=293.15",
        "firevox.smokeIntoFireThreshold=150",
    ],
    classes = [WorkerApplication::class, ItTestConfig::class]
)
class RadiationValidationTest(
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
    val jdbcTemplate: JdbcTemplate,
    val chunkRepository: ChunkRepository,
) : ShouldSpec({

    File("../main/src/main/resources/db.migration/V0.1_RadiationMaterialisedViews.sql")
        .readLines()
        .joinToString(separator = "\n") { it }
        .let(jdbcTemplate::update)

    context("calculate radiation test") {
        val simulationTimeInSeconds = 9000
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION, 0))
        countersRepository.save(Counter(CounterId.MAX_ITERATIONS, (simulationTimeInSeconds / timeStep).toLong()))
        countersRepository.save(Counter(CounterId.PROCESSED_VOXEL_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_VOXELS_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.PROCESSED_RADIATION_PLANES_COUNT, 0))
        countersRepository.save(Counter(CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))
        countersRepository.save(Counter(CounterId.NEXT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT, 0))

        // https://www.researchgate.net/publication/235711653_New_Experimental_Data_for_the_Validation_of_Radiative_Heat_Transfer
        // L: square
        //  side = 197 mm
        //  thickness: 5 mm
        //  HTC2 heat transfer coefficient
        // D: circle
        //  diameter = 182 mm
        //  r = 91 mm
        //  HTC1 heat transfer coefficient

        // C: distance
        //  0.2 * 182 mm ~= 37 | Te = 740 | Tr1 =  | Ambient = 295.8
        //  0.5 * 182 mm = 91  | Te = 720 | Tr1 =  | Ambient = 291.3
        //  1.0 * 182 mm = 182 | Te = 710 | Tr1 =  | Ambient = 291.2
        //  2.0 * 182 mm = 364 | Te = 710 | Tr1 =  | Ambient = 293.3
        //  4.0 * 182 mm = 728 | Te = 709 | Tr1 =  | Ambient = 290.7

        // 1mm^3 = 1 voxel

        // 197 x 197 x (C + 5mm thickness)
//        val (c, Te, Tr, Ta) = arrayOf(37.0, 740.0, 290.0, 290.0) //cd = 0,2
        val (c, Te, Tr, Ta) = arrayOf(182.0, 710.0, 290.0, 290.0) //cd = 1
        val C = c.toInt()

        val squareMaterial = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 8030.0,
            baseTemperature = Tr,
            thermalConductivityCoefficient = 16.27,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 503.0,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
            emissivity = 0.85,
        ).also(physicalMaterialRepository::save)

        val circleMaterial = PhysicalMaterial(
            VoxelMaterial.METAL,
            density = 8030.0,
            baseTemperature = Te,
            thermalConductivityCoefficient = 16.27,
            convectionHeatTransferCoefficient = 0.0,
            specificHeatCapacity = 503.0,
            ignitionTemperature = null,
            burningTime = null,
            timeToIgnition = null,
            autoignitionTemperature = null,
            effectiveHeatOfCombustion = null,
            smokeEmissionPerSecond = null,
            deformationTemperature = null,
        ).also(physicalMaterialRepository::save)

        val airMaterial = physicalMaterialRepository.findByVoxelMaterial(VoxelMaterial.AIR).also {
            it.baseTemperature = Ta
        }.also(physicalMaterialRepository::save)

//        val L = 197
//        val D = 181
        val divisor = 1
        val L = 197 / divisor
        val D = 181 / divisor
        val voxels = (0 until C + 6).flatMap { x ->
            (0 until L).flatMap { y ->
                (0 until L).map { z ->
                    val key = VoxelKey(x, y, z)
                    when {
                        isCircle(key, L/2, D / 2) -> circle(key, circleMaterial, Te)
                        isSquare(key, C) -> square(key, squareMaterial, Tr)
                        else -> air(key, airMaterial, Ta)
                    }
                }
            }
        }.toMutableList()

        val emitterThermometer = VoxelKey(0, 98 / divisor, 98 / divisor)
        val receiverThermometer = VoxelKey(C, 98 / divisor, 98 / divisor)
        virtualThermometerService.create(emitterThermometer)
        virtualThermometerService.create(receiverThermometer)

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

        // then create planes
        val pointsToNormals = listOf(
            emitterThermometer to VoxelKey(1, 0, 0),
            receiverThermometer to VoxelKey(-1, 0, 0),
        )

        val matrix = Array(sizeX) { _ ->
            Array(sizeY) { _ ->
                IntArray(sizeZ) { _ -> 0 }
            }
        }

        voxels.forEach {
            matrix[it.key.x][it.key.y][it.key.z] = it.evenIterationMaterial.voxelMaterial.colorId
        }


        var planes = planeFinder.findPlanes(matrix, pointsToNormals)
            .also {
                countersRepository.set(
                    CounterId.CURRENT_ITERATION_RADIATION_PLANES_TO_PROCESS_COUNT,
                    it.size.toLong()
                )
            }
        planes = planes.let(radiationPlaneRepository::saveAll)
        log.info("${planes.size}")
        radiationPlaneRepository.flush()

        log.info("Persisting all voxels ${voxels.size}s")
        voxelRepository.saveAll(voxels.filterNot { it.evenIterationMaterial.voxelMaterial == airMaterial.voxelMaterial })
        voxelRepository.flush()
        log.info("Finished persisting all voxels")

        should("execute test") {
            val iterationNumber = (simulationTimeInSeconds / timeStep).roundToInt()

            log.info("Start of the processing. Iterations $iterationNumber voxels count: ${voxels.size}")
            synchroniserImpl.synchroniseRadiationResults(0)
            synchroniserImpl.synchroniseRadiationResults(1)
            for (i in 0..iterationNumber) {
                log.info("Iteration: $i")
                virtualThermometerService.updateDirectly(emitterThermometer, i)
                virtualThermometerService.updateDirectly(receiverThermometer, i)
//                val chunk = chunkRepository.fetch(VoxelKey(0, 0, 0), VoxelKey(sizeX - 1, sizeY - 1, sizeZ - 1))
//                calculationService.calculateForChunk(chunk, i)
//                chunkRepository.saveAll(chunk)
                jdbcTemplate.update("update voxels set odd_iteration_temperature = even_iteration_temperature, even_iteration_temperature = odd_iteration_temperature where true;")
                log.info("Started radiation")
                radiationCalculator.calculateFetchingFromDb(0, i)
                radiationCalculator.calculateFetchingFromDb(1, i)
                log.info("Finished radiation")
                synchroniserImpl.synchroniseRadiationResults(i)
                log.info("Finished synchronisation")
                countersRepository.increment(CounterId.CURRENT_ITERATION)
                log.info("Finished increment")
            }

            val result = voxelRepository.findAll().filter { it.evenIterationMaterial.voxelMaterial != VoxelMaterial.AIR }
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
                FileOutputStream("radiation_paper.vox")
            )
            VoxFormatParser.write(
                result.associate {
                    it.key to it.evenIterationMaterial.voxelMaterial.colorId
                },
                Palette.basePalette,
                sizeX,
                sizeY,
                sizeZ,
                FileOutputStream("radiation_material_paper.vox")
            )
            FileOutputStream("receiver.csv").write(virtualThermometerService.getMeasurements(receiverThermometer).toByteArray())
            FileOutputStream("emitter.csv").write(virtualThermometerService.getMeasurements(emitterThermometer).toByteArray())
        }
    }

}) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}

private fun circle(key: VoxelKey, circleMaterial: PhysicalMaterial, baseTemp: Double) = Voxel(
    key,
    evenIterationMaterial = circleMaterial,
    evenIterationTemperature = baseTemp,
    oddIterationMaterial = circleMaterial,
    oddIterationTemperature = baseTemp,
    isBoundaryCondition = true,
)

private fun square(key: VoxelKey, squareMaterial: PhysicalMaterial, baseTemp: Double) = Voxel(
    key,
    evenIterationMaterial = squareMaterial,
    evenIterationTemperature = baseTemp,
    oddIterationMaterial = squareMaterial,
    oddIterationTemperature = baseTemp,
    isBoundaryCondition = false,
)

private fun air(key: VoxelKey, airMaterial: PhysicalMaterial, baseTemp: Double) = Voxel(
    key,
    evenIterationMaterial = airMaterial,
    evenIterationTemperature = baseTemp,
    oddIterationMaterial = airMaterial,
    oddIterationTemperature = baseTemp,
    isBoundaryCondition = true,
)

private fun isCircle(k: VoxelKey, middle: Int, radius: Int): Boolean {
    return k.x == 0 && (pow(k.y - middle, 2) + pow(k.z - middle, 2)) < radius * radius
}

private fun isSquare(k: VoxelKey, C: Int): Boolean = k.x >= C
