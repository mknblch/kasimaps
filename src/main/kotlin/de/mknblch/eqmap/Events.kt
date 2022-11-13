package de.mknblch.eqmap

import java.time.Instant

enum class Origin {
    ANY, DBG, EQLOG
}

// raw event class, emitted by LogParser
data class EqEvent(
    val origin: Origin,
    val time: Instant,
    val timeText: String,
    val text: String,
    val character: String? = null,
    val server: String? = null)

enum class Type(val regex: Regex) {
    SAY(Regex("^([^ ]+) says '(.+)'")),
    TELL(Regex("([^ ]+) tells you, '(.+)'")),
    GROUP(Regex("([^ ]+) tells the group, '(.+)'")),
    GUILD(Regex("([^ ]+) tells the guild, '(.+)'")),
    OOC(Regex("([^ ]+) says out of character, '(.+)'"));
}

data class GameMessageEvent(val type: Type, val to: String, val from: String, val text: String)

data class IRCLocationEvent(
    val player: String,
    val zone: String,
    val x: Double,
    val y: Double,
    val z: Double
)

data class IRCZoneEvent(
    val player: String,
    val zone: String,
)