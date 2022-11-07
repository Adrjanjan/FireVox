package pl.edu.agh.firevox.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.FireVoxProperties
import pl.edu.agh.firevox.model.ModelDescription
import pl.edu.agh.firevox.model.SingleModel
import pl.edu.agh.firevox.vox.VoxFormatParser
import pl.edu.agh.firevox.vox.ParsedVoxFile
import java.io.FileInputStream

@Service
class ModelMergeService {

    @Autowired
    lateinit var fireVoxProperties: FireVoxProperties

    companion object {
        val log: Logger = LoggerFactory.getLogger(ModelMergeService::class.java)
    }

//    fun createModel(modelDescription: ModelDescription): ParsedVoxFile {
//        val parentModelFile = FileInputStream(modelDescription.parentModel.name)
//        val parentModel = VoxFormatParser.read(parentModelFile, fireVoxProperties.maxSize)
//        addChildren(modelDescription.parentModel.childModels, parentModel)
//        parentModel.clipToVoxels()
//        log.info("Finished merging files!")
//        return parentModel
//    }
//
//    private fun addChildren(models: List<SingleModel>, parentModel: ParsedVoxFile) {
//        for (model in models) {
//            log.info("Adding '${model.name}'")
//            val modelIn = FileInputStream(model.name)
//            val readModel = VoxFormatParser.read(modelIn, fireVoxProperties.maxSize)
//            addChildren(model.childModels, readModel)
//            parentModel.addModel(
//                readModel.scale(model.scale),
//                model.positionX ?: 0,
//                model.positionY ?: 0,
//                model.positionZ ?: 0,
//                model.centerX ?: false,
//                model.centerY ?: false,
//                model.centerZ ?: false,
//                model.flipX ?: false,
//                model.flipY ?: false,
//                model.flipZ ?: false,
//                model.rotateX ?: 0,
//                model.rotateY ?: 0,
//                model.rotateZ ?: 0
//            )
//            modelIn.close()
//        }
//    }

}