package pl.edu.agh.firevox.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.simulation.SimulationScene
import pl.edu.agh.firevox.vox.ParsedVoxFile
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream

@Service
class ModelMergeService{

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun createModel(modelDescriptionDto: ModelDescriptionDto): SimulationScene {
        val parentModelFile = FileInputStream(modelDescriptionDto.parentModel.name)
        val parentModel = VoxFormatParser.read(parentModelFile)
        addChildren(modelDescriptionDto.parentModel.childModels, parentModel)
//        parentModel.clipToVoxels()
        // TODO save parent model render options
        // palette, colorIndexMap, materials, renderObjects, cameras
        log.info("Finished merging files!")
        return SimulationScene(parentModel)
    }

    private fun addChildren(models: List<SingleModelDto>, parentModel: ParsedVoxFile) {
        for (model in models) {
            log.info("Adding '${model.name}'")
            val modelIn = FileInputStream(model.name)
            val readModel = VoxFormatParser.read(modelIn)
            addChildren(model.childModels, readModel)
            parentModel.addModel(
                readModel,
//                readModel.scale(model.scale),
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
    }

}