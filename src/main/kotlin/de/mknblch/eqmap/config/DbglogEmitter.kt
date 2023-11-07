package de.mknblch.audiolog.config

import de.mknblch.eqmap.EqEvent
import de.mknblch.eqmap.Origin
import de.mknblch.eqmap.common.LogParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant

@Scope("singleton")
@Service
class DbglogEmitter(
    @Value("\${eq_directory}") val eqDirectory: String,
) : LogParser(File("$eqDirectory/dbg.txt")) {

    @Autowired
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    override fun emit(value: String) {
        val matchResult: MatchResult = lineRegex.matchEntire(value) ?: return
        applicationEventPublisher.publishEvent(
            EqEvent(
                Origin.DBG,
                Instant.now(),
                matchResult.groupValues[1],
                matchResult.groupValues[2]
            )
        )
    }

    companion object {
        private val lineRegex = Regex("([^\t]+)\t(.+)")
    }

}