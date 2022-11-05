package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import java.io.FileOutputStream


class VoxFormatParserTest : ShouldSpec({


    should("read and write model") {
        val input = withContext(Dispatchers.IO) {
            getFile("vox/room.vox")
        }
        val model = VoxFormatParser.read(input, 1024)

        model.sizeX shouldBe 23
        model.sizeY shouldBe 20
        model.sizeZ shouldBe 29

        model.voxels.size shouldBe 2905

        val out = withContext(Dispatchers.IO) {
            FileOutputStream("./vox/test.vox")
        }
        VoxFormatParser.write(model, out)
        withContext(Dispatchers.IO) {
            out.close()
        }
    }

    should("constructRotationFromBits parse bits correctly"){
        // given
        val bits = 1 shl 0 or (2 shl 2) or (0 shl 4) or (1 shl 5) or (1 shl 6)
        // when
        val result = constructRotationFromBits(bits)
        // then
        result shouldBe listOf(
            listOf(0, 1, 0),
            listOf(0, 0, -1),
            listOf(-1, 0, 0),
        )
    }
})

fun ShouldSpec.getFile(name: String) = ClassPathResource(name).inputStream