package pl.edu.agh.firevox.vox

import com.google.common.io.LittleEndianDataInputStream
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.VoxelMaterial.*
import pl.edu.agh.firevox.shared.model.vox.Model
import pl.edu.agh.firevox.shared.model.vox.SceneTree
import pl.edu.agh.firevox.shared.model.vox.chunks.*
import java.io.InputStream

class SceneTreeTest : ShouldSpec({

    val nullInput = LittleEndianDataInputStream(InputStream.nullInputStream())
    val rootTransform = TransformNodeChunk(
        nullInput,
        size = -1, // not needed here
        childSize = 0,
        nodeId = 0,
        nodeAttributes = mapOf(),
        childNodeId = 1,
        layerId = 1,
        numOfFrames = 1,
        framesAttributes = mapOf()
    )

    val letterT = mapOf(
        VoxelKey(1, 1, 0) to 1,
        VoxelKey(1, 1, 1) to 1,
        VoxelKey(0, 1, 2) to 1,
        VoxelKey(1, 1, 2) to 1,
        VoxelKey(2, 1, 2) to 1,
    )

    val twoDotsBottomMiddle = mapOf(
        VoxelKey(1, 0, 0) to 2,
        VoxelKey(1, 2, 0) to 2
    )

    val threeDotsBottom = mapOf(
        VoxelKey(1, 0, 0) to 3,
        VoxelKey(2, 1, 0) to 3,
        VoxelKey(1, 2, 0) to 3
    )

    val letterTMapped = mapOf(
        VoxelKey(1, 1, 0) to WOOD,
        VoxelKey(1, 1, 1) to WOOD,
        VoxelKey(0, 1, 2) to WOOD,
        VoxelKey(1, 1, 2) to WOOD,
        VoxelKey(2, 1, 2) to WOOD,
    )

    should("construct correct scene from simple tree") {
        // given
        val tree = SceneTree(
            mapOf(0 to rootTransform),
            mapOf(),
            mapOf(
                1 to GroupNodeChunk(
                    nullInput,
                    size = -1, //not needed here,
                    childSize = 0,
                    nodeId = 1,
                    nodeAttributes = mapOf(),
                    numOfChildrenNodes = 1,
                    childNodeIds = listOf(3)
                )
            ),
            mapOf(
                3 to ShapeNodeChunk(
                    nullInput,
                    size = -1, // not needed here
                    childSize = 0,
                    nodeId = 2,
                    nodeAttributes = mapOf(),
                    numOfModels = 1,
                    models = listOf(ShapeModel(0, mapOf()))
                )
            ),
        )

        val models = listOf(
            Model(
                SizeChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    sizeX = 3,
                    sizeY = 3,
                    sizeZ = 3,
                ),
                VoxelsChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    numberOfVoxels = 5,
                    voxels = letterT
                )
            )
        )
        // when
        val scene = tree.constructScene(models, 100)
        // then
        scene.size shouldBe 5
        scene shouldBe letterTMapped
    }

    should("construct correct scene from complicated tree with transformations") {
        // given
        val tree = SceneTree(
            mapOf(
                0 to rootTransform,
                4 to TransformNodeChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    nodeId = 4,
                    nodeAttributes = mapOf(),
                    childNodeId = 5,
                    layerId = 1,
                    numOfFrames = 1,
                    framesAttributes = mapOf(
                        0 to TransformProperties(
                            rotation = null,
                            translation = Translation(1, 0, 0),
                            frameIndex = 0,
                        )
                    ),
                )
            ),
            mapOf(),
            mapOf(
                1 to GroupNodeChunk(
                    nullInput,
                    size = -1, //not needed here,
                    childSize = 0,
                    nodeId = 1,
                    nodeAttributes = mapOf(),
                    numOfChildrenNodes = 1,
                    childNodeIds = listOf(3, 4)
                )
            ),
            mapOf(
                3 to ShapeNodeChunk(
                    nullInput,
                    size = -1, // not needed here
                    childSize = 0,
                    nodeId = 2,
                    nodeAttributes = mapOf(),
                    numOfModels = 1,
                    models = listOf(ShapeModel(0, mapOf()))
                ),
                5 to ShapeNodeChunk(
                    nullInput,
                    size = -1, // not needed here
                    childSize = 0,
                    nodeId = 5,
                    nodeAttributes = mapOf(),
                    numOfModels = 1,
                    models = listOf(ShapeModel(1, mapOf()))
                )
            ),
        )

        val models = listOf(
            Model(
                SizeChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    sizeX = 3,
                    sizeY = 3,
                    sizeZ = 3,
                ),
                VoxelsChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    numberOfVoxels = 5,
                    voxels = letterT
                )
            ),
            Model(
                SizeChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    sizeX = 3,
                    sizeY = 3,
                    sizeZ = 3,
                ),
                VoxelsChunk(
                    nullInput,
                    size = -1,
                    childSize = 0,
                    numberOfVoxels = 2,
                    voxels = twoDotsBottomMiddle
                )
            ),

            )
        // when
        val scene = tree.constructScene(models, 100)
        // then
        scene.size shouldBe 7
        scene shouldBe mapOf(
            VoxelKey(1, 1, 0) to WOOD, // T
            VoxelKey(1, 1, 1) to WOOD,
            VoxelKey(0, 1, 2) to WOOD,
            VoxelKey(1, 1, 2) to WOOD,
            VoxelKey(2, 1, 2) to WOOD,

            VoxelKey(2, 0, 0) to WOOD_HEATED, // dots +1x
            VoxelKey(2, 2, 0) to WOOD_HEATED
        )
    }

    context("construct scene from tree with overriding voxels") {
        should("override with newest node") {
            // given
            val tree = SceneTree(
                mapOf(
                    0 to rootTransform,
                    4 to TransformNodeChunk(
                        nullInput,
                        size = -1,
                        childSize = 0,
                        nodeId = 4,
                        nodeAttributes = mapOf(),
                        childNodeId = 5,
                        layerId = 1,
                        numOfFrames = 1,
                        framesAttributes = mapOf(),
                    )
                ),
                mapOf(),
                mapOf(
                    1 to GroupNodeChunk(
                        nullInput,
                        size = -1, //not needed here,
                        childSize = 0,
                        nodeId = 1,
                        nodeAttributes = mapOf(),
                        numOfChildrenNodes = 1,
                        childNodeIds = listOf(3, 4)
                    )
                ),
                mapOf(
                    3 to ShapeNodeChunk(
                        nullInput,
                        size = -1, // not needed here
                        childSize = 0,
                        nodeId = 2,
                        nodeAttributes = mapOf(),
                        numOfModels = 1,
                        models = listOf(ShapeModel(0, mapOf()))
                    ),
                    5 to ShapeNodeChunk(
                        nullInput,
                        size = -1, // not needed here
                        childSize = 0,
                        nodeId = 5,
                        nodeAttributes = mapOf(),
                        numOfModels = 1,
                        models = listOf(ShapeModel(1, mapOf()))
                    )
                ),
            )

            val models = listOf(
                Model(
                    SizeChunk(
                        nullInput,
                        size = -1,
                        childSize = 0,
                        sizeX = 3,
                        sizeY = 3,
                        sizeZ = 3,
                    ),
                    VoxelsChunk(
                        nullInput,
                        size = -1,
                        childSize = 0,
                        numberOfVoxels = 5,
                        voxels = twoDotsBottomMiddle
                    )
                ),
                Model(
                    SizeChunk(
                        nullInput,
                        size = -1,
                        childSize = 0,
                        sizeX = 3,
                        sizeY = 3,
                        sizeZ = 3,
                    ),
                    VoxelsChunk(
                        nullInput,
                        size = -1,
                        childSize = 0,
                        numberOfVoxels = 2,
                        voxels = threeDotsBottom
                    )
                ),

                )
            // when
            val scene = tree.constructScene(models, 100)
            // then
            scene.size shouldBe 3
            scene shouldBe mapOf(
                VoxelKey(1, 0, 0) to WOOD, // dots +1x
                VoxelKey(1, 2, 0) to WOOD,
                VoxelKey(2, 1, 0) to WOOD,
            )
        }
    }

})
