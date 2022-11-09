package de.mknblch.eqmap.config

import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.element.Channel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.MessageDigest
import javax.annotation.PreDestroy
import kotlin.jvm.optionals.getOrNull
import kotlin.text.Charsets.UTF_8

@Service
class NetworkSyncService(
    private val publisher: ApplicationEventPublisher
) {

    private var client: Client? = null

    private var channel: Channel? = null

    @OptIn(ExperimentalStdlibApi::class)
    fun connect(
        host: String,
        chan: String,
        nickName: String,
        chanPassword: String?,
        serverPassword: String?,
    ) {
        client?.shutdown()
        logger.info("init IRCClient(host=$host, nick=$nickName")
        client = Client.builder()
            .server()
            .host(host)
            .password(serverPassword)
            .then()
            .nick(nickName)
            .listeners()
            .input(this::onInMessage)
            .output(this::onOutMessage)
            .exception(this::onException)
            .then()
            .buildAndConnect()
            .also { c ->
                chanPassword?.let { c.addKeyProtectedChannel(chan, chanPassword) } ?: c.addChannel(chan)
            }
        channel = client?.getChannel(chan)?.getOrNull()
    }
    private fun onOutMessage(message: String) {
        logger.info("out: $message")
    }

    private fun onInMessage(message: String) {
        logger.info("in: $message")
    }

    private fun onException(e: Exception) {
        logger.error("IRC Error", e)
    }

    @EventListener
    fun onZoneChange(event: ZoneEvent) {
        val message = "Z,${event.playerName},${event.zone}"
        sendMessage(message)
    }

    @EventListener
    fun onLocationEvent(event: LocationEvent) {
        val message = "L,${event.playerName},${event.x.toInt()},${event.y.toInt()},${event.z.toInt()}"
        sendMessage(message)
    }

    @PreDestroy
    fun onExit() {
        client?.shutdown("exit")
    }

    private fun sendMessage(message: String) {
        channel?.sendMessage(message) ?: kotlin.run {
            logger.warn("message could not be send, no channel available")
        }
    }

    companion object {

        private val md5 = MessageDigest.getInstance("MD5")

        private fun encrypt(clearText: String): String? {
            val data: ByteArray = md5.digest(clearText.toByteArray(UTF_8))
            return BigInteger(1, data).toString(16)
        }
        private val logger = LoggerFactory.getLogger(NetworkSyncService::class.java)
    }
}
