package de.mknblch.eqmap.map

import javafx.scene.paint.Color
import javafx.scene.shape.Line
import kotlin.math.max
import kotlin.math.min

data class MapLine(
    val x1: Double,
    val y1: Double,
    val z1: Double,
    val x2: Double,
    val y2: Double,
    val z2: Double,
    override val color: Color,
    override val zRange: ClosedRange<Double> = (min(z1, z2)..max(z1, z2))
) : Line(x1, y1, x2, y2), MapObject {
    constructor(vararg line: String) : this(
        line[0].toDouble(),
        line[1].toDouble(),
        line[2].toDouble(),
        line[3].toDouble(),
        line[4].toDouble(),
        line[5].toDouble(),
        Color.color(
            line[6].toDouble() / 255.0,
            line[7].toDouble() / 255.0,
            line[8].toDouble() / 255.0,
            1.0
        )
    )

    init {
        stroke = color
    }

    override fun setShow(show: Boolean) {
        opacity = if(show) 1.0 else 0.0
    }

    fun crosses(line: MapLine) {

    }
}