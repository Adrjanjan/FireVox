package pl.edu.agh.firevox

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import pl.edu.agh.firevox.vox.VoxFormatParser
import java.io.FileInputStream
import java.io.FileOutputStream

@SpringBootTest
class FireVoxContextTest {

    @Test
    fun contextLoads() {
    }

    @Test
    fun simpleReadAndWriteTest() {
        val input = FileInputStream("vox/test_materials.vox")
        val model = VoxFormatParser.read(input, 1024)
        val out = FileOutputStream("vox/test_roundtrip.vox")
        VoxFormatParser.write(model, out)
        out.close()
    }
}
