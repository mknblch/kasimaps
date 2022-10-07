package de.mknblch.eqmap.zone

import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text


private const val POI_SIZE = 3

data class SimpleMapPOI(
    val x: Double,
    val y: Double,
    override val color: Color,
    val type: Int,
    val name: String,
    val circle: Circle = Circle(x - POI_SIZE / 2.0, y - POI_SIZE / 2.0, POI_SIZE.toDouble()).also {
        it.stroke = color
        it.fill = color
    },
    val text: Text = Text(x + POI_SIZE + 1, y + POI_SIZE, name).also {
        it.styleClass.add("mapPOIText")
    },
    override val zRange: ClosedRange<Double> = (-Double.MAX_VALUE..Double.MAX_VALUE)
) : MapNode, Group(circle, text) {

    override fun setShow(show: Boolean) {
        if (show) {
            circle.opacity = 1.0
            text.opacity = 1.0
        } else {
            circle.opacity = 0.0
            text.opacity = 0.0
        }
    }

    init {
        styleClass.add("mapPOI")
    }

    companion object {

        fun buildFromString(vararg line: String): SimpleMapPOI {
            return SimpleMapPOI(
                line[0].toDouble(),
                line[1].toDouble(),
                Color.color(line[2].toDouble() / 255.0, line[3].toDouble() / 255.0, line[4].toDouble() / 255.0, 1.0),
                line[5].toInt(),
                line[6].replace('_', ' '),
            )
        }
    }
}
