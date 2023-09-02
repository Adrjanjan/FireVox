package pl.edu.agh.firevox.messaging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration

@RestController
@RequestMapping("messaging")
class MessagingTest(
    val m: MessageSender
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @PostMapping
    fun postVoxelKeyIteration(@RequestBody v: VoxelKeyIteration) {
        log.info("Sending test message $v")
        m.send(v)
    }
}