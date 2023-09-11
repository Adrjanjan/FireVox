package pl.edu.agh.firevox.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockkObject
import org.slf4j.Logger
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.SingleModelDto

class ModelMergeServiceTest : ShouldSpec({
    mockkObject(Logger::class)
    val mms = ModelMergeService()

    val treeOnly = SingleModelDto("src/test/resources/vox/tree.vox")
    val room = SingleModelDto("src/test/resources/vox/room.vox")
    val roadTree = SingleModelDto(
        "src/test/resources/vox/road.vox",
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

    should("merge single model and not change it") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", room)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 117
    }

    should("merge two models without modifications") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTree)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 10
    }

    should("merge two models with offset") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTreeOffset)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 10
    }

    should("merge two models with rotation") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTreeRotation)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 10
    }


})
