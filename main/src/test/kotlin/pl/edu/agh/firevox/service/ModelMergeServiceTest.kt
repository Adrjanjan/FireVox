package pl.edu.agh.firevox.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockkObject
import org.slf4j.Logger
import pl.edu.agh.firevox.model.ModelDescriptionDto
import pl.edu.agh.firevox.model.SingleModelDto
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.simulation.Simulation
import pl.edu.agh.firevox.shared.model.simulation.SingleModel
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream
import java.io.FileOutputStream

val positionOffset = 5

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
                positionX = positionOffset,
                positionY = positionOffset,
                positionZ = positionOffset,
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
    // TODO Add model with size > 256 and multiple elements in SceneTree and test read/write


    should("load single model") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", treeOnly)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 22
        resultModel.sizeY shouldBe 19
        resultModel.sizeZ shouldBe 28

        // this part of the model will be changed in the following tests
        resultModel.voxels[VoxelKey(12, 13, 14)] shouldBe 95
        resultModel.voxels[VoxelKey(13, 11, 30)] shouldBe 95

        resultModel.voxels[VoxelKey(12 + positionOffset, 13 + positionOffset, 14 + positionOffset)] shouldBe null
        resultModel.voxels[VoxelKey(13 + positionOffset, 11 + positionOffset, 30 + positionOffset)] shouldBe 233
    }

    should("merge single model and not change it") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", room)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 118
        resultModel.sizeY shouldBe 121
        resultModel.sizeZ shouldBe 60

        // this part of the model will be changed in the following tests
        resultModel.voxels[VoxelKey(12, 13, 14)] shouldBe null
        resultModel.voxels[VoxelKey(13, 11, 30)] shouldBe 249

        resultModel.voxels[VoxelKey(12 + positionOffset, 13 + positionOffset, 14 + positionOffset)] shouldBe null
        resultModel.voxels[VoxelKey(13 + positionOffset, 11 + positionOffset, 30 + positionOffset)] shouldBe null

        VoxFormatParser.write(
            resultModel.voxels,
            Palette.temperaturePalette,
            resultModel.sizeX,
            resultModel.sizeY,
            resultModel.sizeZ,
            FileOutputStream("room_after_write.vox")
        )
    }

    should("merge two models without modifications") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTree)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 121
        resultModel.sizeY shouldBe 121
        resultModel.sizeZ shouldBe 14

        resultModel.voxels[VoxelKey(12, 13, 14)] shouldBe 95
        resultModel.voxels[VoxelKey(13, 11, 30)] shouldBe 95

        resultModel.voxels[VoxelKey(12 + positionOffset, 13 + positionOffset, 14 + positionOffset)] shouldBe null
        resultModel.voxels[VoxelKey(13 + positionOffset, 11 + positionOffset, 30 + positionOffset)] shouldBe 233

    }

    should("merge two models with offset") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTreeOffset)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 121
        resultModel.sizeY shouldBe 121
        resultModel.sizeZ shouldBe 14

        resultModel.voxels[VoxelKey(12, 13, 14)] shouldBe null
        resultModel.voxels[VoxelKey(13, 11, 30)] shouldBe null

        resultModel.voxels[VoxelKey(12 + positionOffset, 13 + positionOffset, 14 + positionOffset)] shouldBe 95
        resultModel.voxels[VoxelKey(13 + positionOffset, 11 + positionOffset, 30 + positionOffset)] shouldBe 95

        resultModel.voxels[VoxelKey(
            13 + 2 * positionOffset,
            11 + 2 * positionOffset,
            30 + 2 * positionOffset
        )] shouldBe 233
    }

    should("merge two models with rotation") {
        // given
        val modelDescriptionDto = ModelDescriptionDto("out.vox", roadTreeRotation)

        // when
        val resultModel = mms.createModel(modelDescriptionDto)

        // then
        resultModel.sizeX shouldBe 121
        resultModel.sizeY shouldBe 121
        resultModel.sizeZ shouldBe 14


        resultModel.voxels[VoxelKey(11, 10, 10)] shouldBe 95 // 12, 13, 14 after rotation
        resultModel.voxels[VoxelKey(13, 11, 30)] shouldBe null

        resultModel.voxels[VoxelKey(12 + positionOffset, 13 + positionOffset, 14 + positionOffset)] shouldBe null
        resultModel.voxels[VoxelKey(13 + positionOffset, 11 + positionOffset, 30 + positionOffset)] shouldBe null
    }

})
