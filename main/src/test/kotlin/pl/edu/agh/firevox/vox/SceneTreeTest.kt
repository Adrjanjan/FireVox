package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe

class SceneTreeTest : ShouldSpec({

    val tree = SceneTree(
        TODO()
    )

    // TODO
    should("construct correct scene from tree") {
        // given
        val models = listOf<Model>(
            TODO()
        )
        // when
        val scene = tree.constructScene(models, 100)
        // then
        scene.size shouldBe 1
        scene shouldContainInOrder listOf(
            TODO()
        )

    }


})
