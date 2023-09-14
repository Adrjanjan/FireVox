package pl.edu.agh.firevox.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.vox.ParsedVoxFile
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream

@Service
class ModelMergeService{

    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    fun createModel(modelDescriptionDto: ModelDescriptionDto): ParsedVoxFile {
        val parentModelFile = FileInputStream(modelDescriptionDto.parentModel.name)
        val parentModel = VoxFormatParser.read(parentModelFile)
        addChildren(modelDescriptionDto.parentModel.childModels, parentModel)
//        parentModel.clipToVoxels()

        // TODO save parent model render options // for now skip - only hardcoded palettes can be used
        // palette, colorIndexMap, materials, renderObjects, cameras
        log.info("Finished merging files!")
        return parentModel
    }

    /**
     * Right now child models are stripped to the size of the parent in all directions
     * - may be it should be changed to expand parent's size
     */
    private fun addChildren(models: List<SingleModelDto>, parentModel: ParsedVoxFile) {
        for (model in models) {
            val size  = parentModel.voxels.size
            log.info("Adding '${model.name} to parent with size $size '")
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
            log.info("Addedd ${model.name} with size ${readModel.voxels.size} to parent with final size ${parentModel.voxels.size} number of new voxels = [${parentModel.voxels.size - readModel.voxels.size}]")
        }
    }

}