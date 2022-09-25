package de.mknblch.eqmap.config

import de.mknblch.audiolog.config.EqlogEmitter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.io.path.isRegularFile

@Configuration
class DirectoryWatcherConfig(@Value("\${eq_directory}") val eqDirectory: String) {

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    private val activeParser: MutableMap<Path, EqlogEmitter> = HashMap()

    private val timer = Timer()

    private lateinit var timerTask: TimerTask

    @PostConstruct
    fun init() {
        timerTask = timer.scheduleAtFixedRate(0, 5_000L) {
            Files.walk(Paths.get(eqDirectory, "/Logs/")).filter { it.isRegularFile() }.forEach {
                val matchEntire = FILE_REGEX.matchEntire(it.fileName.toString()) ?: return@forEach
                val character: String = matchEntire.groupValues[1]
                val server: String = matchEntire.groupValues[2]
                activeParser.computeIfAbsent(it) { path -> createParser(path, character, server) }
            }
        }
    }

    @PreDestroy
    fun tearDown() {
        timerTask.cancel()
        timer.cancel()
        activeParser.values.forEach {
            it.close()
        }
    }

    private fun createParser(path: Path, character: String, server: String): EqlogEmitter {
        logger.info("parsing events for $character on $server")
        return EqlogEmitter(applicationEventPublisher, path, character, server)
    }

    companion object {

        private val FILE_REGEX = Regex("eqlog_([^_]+)_([^.]+)\\.txt")

        private val logger: Logger = LoggerFactory.getLogger(DirectoryWatcherConfig::class.java)
    }
}