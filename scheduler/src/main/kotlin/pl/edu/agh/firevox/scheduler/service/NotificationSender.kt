package pl.edu.agh.firevox.scheduler.service

import com.google.cloud.spring.pubsub.core.PubSubTemplate
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.messaging.MessageHandler
import org.springframework.stereotype.Component
import pl.edu.agh.pwch.shop.shareddto.notification.EmailDto

@Component
class NotificationSender {
    @Bean
    fun pubSubOutputChannel() = DirectChannel()

    @Bean
    @ServiceActivator(inputChannel = "pubSubOutputChannel")
    fun messageSender(pubSubTemplate: PubSubTemplate): MessageHandler {
        val adapter = PubSubMessageHandler(pubSubTemplate, TOPIC_NAME)
        adapter.setSuccessCallback { message, _ ->
            LOGGER.info("Message $message was sent successfully.")
        }
        adapter.setFailureCallback { error, _ ->
            LOGGER.info("There was an error ${error.cause} sending the message.")
        }
        return adapter
    }

    @MessagingGateway(defaultRequestChannel = "pubSubOutputChannel")
    interface PubSubEmailGateway {
        fun sendEmailToPubSub(email: EmailDto)
    }

    companion object {
        private val LOGGER: Log = LogFactory.getLog(NotificationSender::class.java)
        private const val TOPIC_NAME = "notification-topic"
    }
}
