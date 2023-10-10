package pl.edu.agh.firevox.messaging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import pl.edu.agh.firevox.shared.model.VoxelKeyIteration

@Service
class VoxelProcessingMessageSender(
    private val streamBridge: StreamBridge
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private val topicName = "voxels-topic"

    fun send(ki: VoxelKeyIteration) {
        log.info("Sending on queue $topicName key $ki")
        streamBridge.send(topicName, ki)
    }

}