package de.mknblch.eqmap.map

import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text

private const val POI_SIZE = 3

data class MapPOI(
    val x: Double,
    val y: Double,
    val z: Double,
    override val color: Color,
    val type: Int,
    val name: String,
    val circle: Circle = Circle(x - POI_SIZE / 2.0, y - POI_SIZE / 2.0, POI_SIZE.toDouble()).also {
        it.stroke = color
        it.fill = color
    },
    val text: Text = Text(x + POI_SIZE + 1, y + POI_SIZE, name),
    override val zRange: ClosedRange<Double> = (z..z)
) : MapObject, Group(circle, text) {

    constructor(vararg line: String) : this(
        line[0].toDouble(),
        line[1].toDouble(),
        line[2].toDouble(),
        Color.color(line[3].toDouble() / 255.0, line[4].toDouble() / 255.0, line[5].toDouble() / 255.0, 1.0),
        line[6].toInt(),
        line[7].replace('_', ' '),
    )

    override fun setShow(show: Boolean) {
        if (show) {
            circle.opacity = 1.0
            text.opacity = 1.0
        } else {
            circle.opacity = 0.0
            text.opacity = 0.0
        }
    }
}
