package de.mknblch.eqmap.config

import de.mknblch.eqmap.EqEvent
import de.mknblch.eqmap.Origin
import de.mknblch.eqmap.fx.MapPane
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant

// /who received
data class WhoEvent(val time: Instant, val zone: String, val players: Set<String>)

@Service
class WhoEventMapper {

    @Qualifier("whoMapping")
    @Autowired
    private lateinit var whoMapping: Map<String, String>

    private val players = HashSet<String>()
    private var active = false

    @EventListener
    fun map(event: EqEvent): WhoEvent? {
        if (event.origin != Origin.EQLOG) return null
        if (event.text == "---------------------------") return null
        // start who
        if (event.text == "Players on EverQuest:") {
            players.clear()
            active = true
            return null
        }
        if (!active) return null
        // end who
        END_REGEX.matchEntire(event.text)?.also {
            active = false
            val rawZone = it.groupValues[2].lowercase()
            return WhoEvent(event.time, whoMapping[rawZone] ?: "unknown", players)
        }
        players.add(event.text.trim())
        return null
    }

    companion object {

        private val logger = LoggerFactory.getLogger(WhoEventMapper::class.java)

        private val END_REGEX = Regex("There (?:is|are) (\\d+) players? in (.+).")
    }
}