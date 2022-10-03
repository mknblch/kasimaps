package de.mknblch.eqmap.zone

import javafx.scene.paint.Color

interface MapNode {

    val color: Color

    val zRange: ClosedRange<Double>

    fun inRangeTo(range: ClosedRange<Double>) = zRange.contains(range.start) ||
            zRange.contains(range.endInclusive) ||
            range.contains(zRange.start) ||
            range.contains(zRange.endInclusive)

    fun setShow(show: Boolean)

    companion object {

        fun fromString(l: String): MapNode? {
            val head = l[0].toString()
            val line: Array<String> = l.removeRange(0, 1)
                .split(",")
                .map(String::trim)
                .toTypedArray()
            return try {
                when (head) {
                    "L" -> MapLine.buildFromLine(*line)
                    "P" -> MapPOI.buildFromString(*line)
                    "K" -> SimpleMapPOI.buildFromString(*line)
                    "#" -> null // skip
                    else -> throw IllegalArgumentException("invalid header '$head'")
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("in ${line.toList()} from '$l'", e)
            }
        }
    }
}