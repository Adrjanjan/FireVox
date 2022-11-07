package pl.edu.agh.firevox.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.mockkObject
import org.slf4j.Logger
import pl.edu.agh.firevox.FireVoxProperties
import pl.edu.agh.firevox.model.ModelDescription
import pl.edu.agh.firevox.model.SingleModel

class ModelMergeServiceTest : ShouldSpec({
    mockkObject(Logger::class)
    val fireVoxProperties: FireVoxProperties = mockk(relaxed = true)
    val mms = ModelMergeService()

    beforeAny {
        mms.fireVoxProperties = fireVoxProperties
    }

    val treeOnly = SingleModel("./vox/tree.vox")
    val roadTree = SingleModel(
        "./vox/road.vox",
        listOf(treeOnly)
    )
    val roadTreeOffset = roadTree.copy(
        childModels = listOf(
            treeOnly.copy(
                positionX = 5,
                positionY = 5,
                positionZ = 5,
            )
        )
    )
    val roadTreeRotation = roadTree.copy(
        childModels = listOf(
            treeOnly.copy(
                rotateX = 90,
                rotateY = 180,
                rotateZ = 270,
            )
        )
    )

//    should("merge single model and not change it") {
//        // given
//        val modelDescription = ModelDescription("out.vox", treeOnly)
//
//        // when
//        val resultModel = mms.createModel(modelDescription)
//
//        // then
//        resultModel.sizeX shouldBe 10
//    }
//
//    should("merge two models without modifications") {
//        // given
//        val modelDescription = ModelDescription("out.vox", roadTree)
//
//        // when
//        val resultModel = mms.createModel(modelDescription)
//
//        // then
//        resultModel.sizeX shouldBe 10
//    }
//
//    should("merge two models with offset") {
//        // given
//        val modelDescription = ModelDescription("out.vox", roadTreeOffset)
//
//        // when
//        val resultModel = mms.createModel(modelDescription)
//
//        // then
//        resultModel.sizeX shouldBe 10
//    }
//
//    should("merge two models with rotation") {
//        // given
//        val modelDescription = ModelDescription("out.vox", roadTreeRotation)
//
//        // when
//        val resultModel = mms.createModel(modelDescription)
//
//        // then
//        resultModel.sizeX shouldBe 10
//    }


})
