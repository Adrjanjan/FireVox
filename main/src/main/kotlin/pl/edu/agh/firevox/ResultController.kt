package pl.edu.agh.firevox

import jakarta.servlet.http.HttpServletResponse
import jakarta.websocket.server.PathParam
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import pl.edu.agh.firevox.results.ResultGenerator
import pl.edu.agh.firevox.shared.model.simulation.PaletteType

@RestController
@RequestMapping("results")
class ResultController(
    private val resultGenerator: ResultGenerator
) {

    @GetMapping
    fun getResult(@PathParam("palette") palette: PaletteType, response: HttpServletResponse) =
        resultGenerator.getResult(palette, response.outputStream)
            .let {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=${it.second}.vox")
                response.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                response.outputStream.flush()
            }
}