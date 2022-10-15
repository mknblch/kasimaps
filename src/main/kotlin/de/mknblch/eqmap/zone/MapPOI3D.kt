package de.mknblch.eqmap.zone

import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text

private const val POI_SIZE = 3

data class MapPOI3D(
    val x: Double,
    val y: Double,
    val z: Double,
    override val color: Color,
    val type: Int,
    override val name: String,
    val circle: Circle = Circle(x, y, POI_SIZE.toDouble()).also {
        it.stroke = color
        it.fill = color
    },
    val text: Text = Text(x + POI_SIZE + 3, y + 5, name),
    override val zRange: ClosedRange<Double> = (z..z)
) : MapNode, POI, Group(circle, text) {

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

    override fun getViewColor(): Color? {
        return text.fill as? Color
    }

    override fun setViewColor(color: Color) {
        text.fill = color
        circle.stroke = color.invert()
        circle.fill = color.invert()
    }

    companion object {

        fun buildFromString(vararg line: String): MapPOI3D {
            return MapPOI3D(
                line[0].toDouble(),
                line[1].toDouble(),
                line[2].toDouble(),
                Color.color(line[3].toDouble() / 255.0, line[4].toDouble() / 255.0, line[5].toDouble() / 255.0, 1.0),
                line[6].toInt(),
                line[7].replace('_', ' '),
            )
        }
    }
}
