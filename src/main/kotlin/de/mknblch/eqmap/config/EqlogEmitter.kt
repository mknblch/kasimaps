package de.mknblch.audiolog.config

import de.mknblch.eqmap.EqEvent
import de.mknblch.eqmap.Origin
import de.mknblch.eqmap.common.LogParser
import org.springframework.context.ApplicationEventPublisher
import java.io.File
import java.nio.file.Path
import java.time.Instant

class EqlogEmitter(
    val applicationEventPublisher: ApplicationEventPublisher,
    val file: File,
    val character: String,
    val server: String
) : LogParser(file) {

    override fun emit(value: String) {
        val matchResult: MatchResult = lineRegex.matchEntire(value) ?: return
        applicationEventPublisher.publishEvent(
            EqEvent(
                origin = Origin.EQLOG,
                time = Instant.now(),
                timeText = matchResult.groupValues[1],
                text = matchResult.groupValues[2],
                character = character,
                server = server
            )
        )
    }

    companion object {
        private val lineRegex = Regex("\\[[a-zA-Z]+ [a-zA-Z]+ \\d+ ([^ ]+) \\d+\\] (.+)")
    }
}