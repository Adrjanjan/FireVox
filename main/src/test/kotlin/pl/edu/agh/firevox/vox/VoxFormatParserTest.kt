package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream
import java.io.FileOutputStream


class VoxFormatParserTest : ShouldSpec({


    should("read and write model") {
        val input = withContext(Dispatchers.IO) {
            getFile("vox/tree.vox")
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
})

fun ShouldSpec.getFile(name: String) = ClassPathResource(name).inputStream