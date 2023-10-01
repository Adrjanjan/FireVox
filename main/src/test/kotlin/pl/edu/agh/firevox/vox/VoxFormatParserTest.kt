package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.InternalPlatformDsl.toArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.shared.model.vox.chunks.constructRotationFromBits
import java.io.FileOutputStream


class VoxFormatParserTest : ShouldSpec({


    should("read and write the same model") {
        // given
        val input = withContext(Dispatchers.IO) {
            getFile("vox/bigger_than_256_3.vox")
        }
        // when
        val model = VoxFormatParser.read(input)
        // then
        val outputStream = FileOutputStream("bigger_than_256_3_out.vox")
        VoxFormatParser.write(
            model.voxels,
            Palette.temperaturePalette,
            model.sizeX,
            model.sizeY,
            model.sizeZ,
            outputStream
        )
        1 == 1
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