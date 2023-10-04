package pl.edu.agh.firevox.results

import jakarta.servlet.ServletOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.CustomVoxelRepository
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.PaletteType
import pl.edu.agh.firevox.shared.model.simulation.SimulationsRepository
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import java.io.OutputStream

@Service
class ResultGenerator(
    private val voxelRepository: CustomVoxelRepository,
    private val simulationsRepository: SimulationsRepository
) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun getResult(palette: PaletteType, outputStream: ServletOutputStream): Pair<OutputStream, String> {
        val simulation = simulationsRepository.findAll()[0]
        log.info("Starting to write result of simulation ${simulation.name}")
        val voxels = voxelRepository.findAllForPalette(palette)
        VoxFormatParser.write(
            voxels,
            Palette.palettes[palette]!!,
            simulation.sizeX,
            simulation.sizeY,
            simulation.sizeZ,
            outputStream
        )
        return outputStream to simulation.name
    }

}