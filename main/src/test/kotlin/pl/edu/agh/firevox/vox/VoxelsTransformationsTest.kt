package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.firevox.vox.VoxelsTransformations.rotateVoxels
import pl.edu.agh.firevox.vox.VoxelsTransformations.translate
import pl.edu.agh.firevox.vox.chunks.Rotation
import pl.edu.agh.firevox.vox.chunks.Translation

internal class VoxelsTransformationsTest : ShouldSpec({

    val baseCube = mutableListOf(
        Voxel(0, 0, 0, 1),
        Voxel(1, 1, 1, 2),
        Voxel(2, 2, 2, 3),
    )

    should("translateVoxels") {
        baseCube.translate(Translation(1, 2, 3)) shouldBe listOf(
            Voxel(1, 2, 3, 1),
            Voxel(2, 3, 4, 2),
            Voxel(3, 4, 5, 3),
        )
    }

    should("rotateVoxels") {
        baseCube.rotateVoxels(
            Rotation(
                listOf(
                    listOf(1, 0, 0),
                    listOf(0, 0, -1),
                    listOf(0, 1, 0),
                )
            )
        ) shouldBe listOf(
            Voxel(1, 2, 3, 1),
            Voxel(2, 3, 4, 2),
            Voxel(3, 4, 5, 3),
        )
    }
})