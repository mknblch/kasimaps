package de.mknblch.eqmap.config

import de.mknblch.eqmap.EqEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

enum class Type(val regex: Regex) {
    SAY(Regex("^([^ ]+) says '(.+)'")),
    TELL(Regex("([^ ]+) tells you, '(.+)'")),
    GROUP(Regex("([^ ]+) tells the group, '(.+)'")),
    GUILD(Regex("([^ ]+) tells the guild, '(.+)'")),
    OOC(Regex("([^ ]+) says out of character, '(.+)'"));
}

data class MessageEvent(val type: Type, val to: String, val from: String, val text: String)

@Service
class MessageService {

    @EventListener
    fun onEvent(event: EqEvent) : MessageEvent? {
        Type.values().forEach {
            val match = it.regex.matchEntire(event.text)
            if (match != null) {
                return MessageEvent(it, event.character ?: "?", match.groupValues[1], match.groupValues[2])
            }
        }
        return null
    }
}