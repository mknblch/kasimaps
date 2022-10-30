package de.mknblch.eqmap.config

import de.mknblch.audiolog.config.EqlogEmitter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Service
class DirectoryWatcherService() {

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    private val activeParser: MutableMap<Path, EqlogEmitter> = HashMap()

    private val timer = Timer()

    private var timerTask: TimerTask? = null

    private var eqDirectory: File? = null

    private fun buildTimerTask(eqDirectory: File) = timer.scheduleAtFixedRate(0, 5_000L) {
        if (!Paths.get(eqDirectory.absolutePath, "/Logs/").exists()) {
            logger.warn("eq_directory '${eqDirectory.absolutePath}/Logs/' not found!")
            return@scheduleAtFixedRate
        }
        this@DirectoryWatcherService.eqDirectory = eqDirectory
        Files.walk(Paths.get(eqDirectory.absolutePath, "/Logs/")).filter { it.isRegularFile() }.forEach {
            val matchEntire = FILE_REGEX.matchEntire(it.fileName.toString()) ?: return@forEach
            val character: String = matchEntire.groupValues[1]
            val server: String = matchEntire.groupValues[2]
            activeParser.computeIfAbsent(it) { path -> createParser(path, character, server) }
        }
    }

    @PreDestroy
    fun tearDown() {
        timerTask?.cancel()
        timer.cancel()
        activeParser.values.forEach {
            it.close()
        }
    }

    fun reset() {
        timerTask?.cancel()
        activeParser.values.forEach {
            it.close()
        }
        activeParser.clear()
        eqDirectory?.let {
            timerTask = buildTimerTask(it)
        }
    }

    fun start(eqDirectory: File) {
        reset()
        timerTask = buildTimerTask(eqDirectory)
    }

    private fun createParser(path: Path, character: String, server: String): EqlogEmitter {
        logger.info("creating parser for $character on $server")
        return EqlogEmitter(applicationEventPublisher, path, character, server)
    }

    companion object {

        private val FILE_REGEX = Regex("eqlog_([^_]+)_([^.]+)\\.txt")

        private val logger: Logger = LoggerFactory.getLogger(DirectoryWatcherService::class.java)
    }
}