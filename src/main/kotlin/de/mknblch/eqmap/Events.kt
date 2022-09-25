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

interface GameEvent