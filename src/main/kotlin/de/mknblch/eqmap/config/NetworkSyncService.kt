package de.mknblch.eqmap.config

import de.mknblch.eqmap.StatusEvent
import net.engio.mbassy.listener.Handler
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType.INSECURE
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType.SECURE
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.MessageDigest
import javax.annotation.PreDestroy
import javax.net.ssl.TrustManagerFactory
import kotlin.text.Charsets.UTF_8


data class IRCNetworkConfig(
    val host: String,
    val port: Int,
    val secure: Boolean,
    val chan: String,
    val nickName: String,
    val chanPassword: String?,
    val serverPassword: String?,
)

@Service
class NetworkSyncService(
    private val publisher: ApplicationEventPublisher
) {

    private var zone: String? = null

    private var client: Client? = null


    @OptIn(ExperimentalStdlibApi::class)
    fun connect(config: IRCNetworkConfig) {
        client?.shutdown()
        logger.info("init IRCClient using $config")

        client = Client.builder()
            .server()
            .secureTrustManagerFactory(TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()))
            .password(config.serverPassword)
            .host(config.host)
            .port(config.port, if (config.secure) SECURE else INSECURE)
            .then()
            .nick(config.nickName)
            .listeners()
            .input(this::onInMessage)
            .output(this::onOutMessage)
            .exception(this::onException)
            .then()
            .buildAndConnect()
            .also { c ->
                c.eventManager.registerEventListener(this)
                config.chanPassword?.let { c.addKeyProtectedChannel(config.chan, it) } ?: c.addChannel(config.chan)
            }
    }

    @Handler
    private fun onJoin(event: ChannelJoinEvent) {

        logger.debug(event.toString())
        publisher.publishEvent(StatusEvent("Joined ${event.channel.name}"))
    }

    fun disconnect() {
        client?.shutdown("exit")
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
        this.zone = event.zone
        val message = "Z,${event.playerName},${event.zone}"
        sendMessage(message)
    }

    @EventListener
    fun onLocationEvent(event: LocationEvent) {
        if (zone == null) return
        val message = "L,${event.playerName},$zone,${event.x.toInt()},${event.y.toInt()},${event.z.toInt()}"
        sendMessage(message)
    }

    @PreDestroy
    fun onExit() {
        client?.shutdown("exit")
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun sendMessage(message: String) {

        client?.channels?.firstOrNull()?.sendMessage(message) ?: kotlin.run {
            logger.warn("message could not be send, no channel available")
        }
    }

    fun setZone(shortName: String) {
        this.zone = shortName
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
