package pl.edu.agh.firevox.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.FireVoxProperties
import pl.edu.agh.firevox.model.ModelDescription
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.logging.Logger


@Service
class ModelMergeService {

    @Autowired
    lateinit var fireVoxProperties: FireVoxProperties

    companion object {
        val log: Logger = Logger.getGlobal()
    }

    fun createModel(modelDescription: ModelDescription) {
        val output = FileOutputStream(modelDescription.outputName)

        val parentModelFile = FileInputStream(modelDescription.parentModel.name)
        val parent = VoxFormatParser.read(parentModelFile, fireVoxProperties.maxSize)

        for (model in modelDescription.parentModel.childModels) {
            log.info("Adding '${model.name}'")
            val modelIn = FileInputStream(model.name)
            parent.add(
                VoxFormatParser.read(modelIn, fireVoxProperties.maxSize).scale(model.scale),
                model.positionX ?: 0,
                model.positionY ?: 0,
                model.positionZ ?: 0,
                model.centerX ?: false,
                model.centerY ?: false,
                model.centerZ ?: false,
                model.flipX ?: false,
                model.flipY ?: false,
                model.flipZ ?: false,
                model.rotateX ?: 0,
                model.rotateY ?: 0,
                model.rotateZ ?: 0
            )
            modelIn.close()
        }
        parent.clipToVoxels()
//        if (forViewer) {
//            log.info("Writing out vox files to '${modelDescription.outputName}'")
//            parent.splitIntoTiles(Path.of(modelDescription.outputName).parent.name, FireVoxProperties.maxSize)
//            log.info("Drag the file '${modelDescription.outputName}.txt' into the MagicaVoxel Viewer to render.")
//        } else {
        log.info("Writing vox result to '${modelDescription.outputName}'")
        VoxFormatParser.write(parent, output)
        output.close()
//        }
        log.info("Finished merging files!")
    }

}