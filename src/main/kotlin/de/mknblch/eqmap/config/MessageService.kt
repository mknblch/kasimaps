package de.mknblch.eqmap.config

import de.mknblch.eqmap.EqEvent
import de.mknblch.eqmap.GameMessageEvent
import de.mknblch.eqmap.Type
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class MessageService {

    @EventListener
    fun onEvent(event: EqEvent) : GameMessageEvent? {
        Type.values().forEach {
            val match = it.regex.matchEntire(event.text)
            if (match != null) {
                return GameMessageEvent(it, event.character ?: "?", match.groupValues[1], match.groupValues[2])
            }
        }
        return null
    }
}