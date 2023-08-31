package pl.edu.agh.firevox.messaging

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration

@Controller("messaging")
class MessagingTest(
    val m: MessageSender
) {

    @PostMapping
    fun postVoxelKeyIteration(@RequestBody v: VoxelKeyIteration) {
        m.send(v)
    }
}