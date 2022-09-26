package de.mknblch.eqmap.map

import javafx.scene.Node
import javafx.scene.paint.Color

interface MapObject {

    val color: Color

    val zRange: ClosedRange<Double>

    fun inRangeTo(range: ClosedRange<Double>) = zRange.contains(range.start) ||
            zRange.contains(range.endInclusive) ||
            range.contains(zRange.start) ||
            range.contains(zRange.endInclusive)

    fun show()

    fun hide()

    companion object {

        fun fromString(l: String): MapObject? {
            val head = l[0].toString()
            val line: Array<String> = l.removeRange(0, 1)
                .split(",")
                .map(String::trim)
                .toTypedArray()
            return try {
                when (head) {
                    "L" -> MapLine(*line)
                    "P" -> MapPOI(*line)
                    "#" -> null // skip
                    else -> throw IllegalArgumentException("invalid header '$head'")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("in ${line.toList()} from '$l'", e)
            }
        }
    }
}
