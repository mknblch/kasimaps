package de.mknblch.eqmap.config

import de.mknblch.eqmap.EqEvent
import de.mknblch.eqmap.Origin
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

open class PlayerEvent(val player: String)

data class ZoneEvent(val zone: String, val playerName: String) : PlayerEvent(playerName)

data class LocationEvent(val x: Double, val y: Double, val z: Double, val playerName: String) : PlayerEvent(playerName)

@Configuration
class EventMapperConfig {

    private val zoneRegex = Regex("You have entered (.+)\\.")
    private val locRegex = Regex("Your Location is ([^,]+), ([^,]+), ([^,]+)")

    @EventListener
    fun onEqEvent(event: EqEvent): PlayerEvent? {
        if (event.origin == Origin.EQLOG) {
            zoneRegex.matchEntire(event.text)?.run {
                return ZoneEvent(groupValues[1], event.character ?: "?")
            }

            locRegex.matchEntire(event.text)?.run {
                val y = -groupValues[1].toDouble()
                val x = -groupValues[2].toDouble()
                val z = groupValues[3].toDouble()
                return LocationEvent(x, y, z, event.character ?: "?")
            }
        }
        return null
    }

}