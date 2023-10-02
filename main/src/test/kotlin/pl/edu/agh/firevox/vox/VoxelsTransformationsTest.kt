package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.vox.VoxelsTransformations.rotateVoxels
import pl.edu.agh.firevox.shared.model.vox.VoxelsTransformations.translate
import pl.edu.agh.firevox.shared.model.vox.chunks.Rotation
import pl.edu.agh.firevox.shared.model.vox.chunks.Translation

internal class VoxelsTransformationsTest : ShouldSpec({

    val baseCube = mutableMapOf(
        VoxelKey(0, 0, 0) to 1,
        VoxelKey(1, 1, 1) to 2,
        VoxelKey(2, 2, 2) to 3,
    )

    should("translateVoxels") {
        baseCube.translate(Translation(1, 2, 3)) shouldBe mapOf(
            VoxelKey(1, 2, 3) to 1,
            VoxelKey(2, 3, 4) to 2,
            VoxelKey(3, 4, 5) to 3,
        )
    }

    should("rotateVoxels") {
        mutableMapOf(
            VoxelKey(0, 0, 0) to 1,
            VoxelKey(0, 0, 1) to 2,
            VoxelKey(0, 0, 2) to 3,
        ).rotateVoxels(
            Rotation(
                listOf(
                    listOf(1, 0, 0),
                    listOf(0, 0, -1),
                    listOf(0, 1, 0),
                )
            )
        ) shouldContainAll mapOf(
            VoxelKey(0, 2, 0) to 1,
            VoxelKey(0, 1, 0) to 2,
            VoxelKey(0, 0, 0) to 3,
        )
    }
})