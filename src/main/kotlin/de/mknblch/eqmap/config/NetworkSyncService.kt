package de.mknblch.eqmap.config

import de.mknblch.eqmap.StatusEvent
import net.engio.mbassy.listener.Handler
import org.kitteh.irc.client.library.Client
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType.INSECURE
import org.kitteh.irc.client.library.defaults.element.mode.DefaultChannelMode
import org.kitteh.irc.client.library.element.mode.ChannelMode
import org.kitteh.irc.client.library.element.mode.Mode
import org.kitteh.irc.client.library.element.mode.ModeStatus
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent
import org.kitteh.irc.client.library.event.channel.RequestedChannelJoinCompleteEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.MessageDigest
import javax.annotation.PreDestroy
import kotlin.text.Charsets.UTF_8


data class IRCNetworkConfig(
    val host: String,
    val port: Int,
    val chan: String,
    val nickName: String,
    val encryptionPassword: String?,
    val chanPassword: String?,
)

@Service
class NetworkSyncService(
    private val publisher: ApplicationEventPublisher
) {

    private var config: IRCNetworkConfig? = null
    private var zone: String? = null
    private var client: Client? = null
    private var encoder: Encoder? = null

    fun connect(config: IRCNetworkConfig) {
        this.config = config
        client?.shutdown()
        logger.info("initializing IRCClient using $config")
        encoder = Encoder(config.encryptionPassword ?: "meow")

        client = Client.builder()
            .server()
            .host(config.host)
            .port(config.port, INSECURE)
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
    private fun onJoin(event: RequestedChannelJoinCompleteEvent) {
        if (zone == null) return
        val message = "${event.actor.nick} joined ${event.channel.name}"
        config?.chanPassword?.also {
            logger.info("setting channel password $it")
            event.channel.commands().mode().add(ModeStatus.Action.ADD, DefaultChannelMode(client!!, 'k', ChannelMode.Type.C_PARAMETER_ON_SET), it).execute()
        }
        logger.info(message)
        publisher.publishEvent(StatusEvent(message))
    }


    @Handler
    private fun onIrcMessage(event: ChannelMessageEvent) {
//        if (zone == null) return
//        if (event.client.nick == event.actor.nick) return
        logger.info(event.toString())
        val message = encoder?.decrypt(event.message) ?: return
        logger.info("Message from ${event.actor.nick}: $message")
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
        val encrypted = encoder?.encrypt(message) ?: return
        client?.channels?.firstOrNull()?.sendMessage(encrypted) ?: kotlin.run {
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
