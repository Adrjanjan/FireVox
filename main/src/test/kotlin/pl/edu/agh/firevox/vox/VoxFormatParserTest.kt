package pl.edu.agh.firevox.vox

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.InternalPlatformDsl.toArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import pl.edu.agh.firevox.shared.model.VoxelKey
import pl.edu.agh.firevox.shared.model.simulation.Palette
import pl.edu.agh.firevox.shared.model.vox.VoxFormatParser
import pl.edu.agh.firevox.shared.model.vox.chunks.constructRotationFromBits
import java.io.FileOutputStream


class VoxFormatParserTest : ShouldSpec({


    should("read and write the same model") {
        // given
        val input = withContext(Dispatchers.IO) {
            getFile("vox/radiation_test.vox")
        }
        // when
        val model = VoxFormatParser.read(input)
        // then
        val outputStream = FileOutputStream("radiation_test_out.vox")
        VoxFormatParser.write(
            model.voxels.map { it.key to 19 }.associate { it.first to it.second }.toMutableMap().also { it[VoxelKey(10, 10, 10)] = 0 },
            Palette.basePalette,
            model.sizeX,
            model.sizeY,
            model.sizeZ,
            outputStream
        )
        1 == 1

//         given
//        val input2 = withContext(Dispatchers.IO) {
//            getFile("vox/rotatebug.vox")
//        }
//        // given
//        val input3 = withContext(Dispatchers.IO) {
//            getFile("vox/rotatebug_out.vox")
//        }
//        // when
//        val model2 = VoxFormatParser.read(input2)
//        val model3 = VoxFormatParser.read(input3)
//
//        model2.voxels.equals(model3.voxels)

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