package de.mknblch.eqmap.zone

import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text
import kotlin.math.max

class MapPOI2D(
    val x: Double,
    val y: Double,
    override val color: Color,
    val size: Int,
    override val name: String,
    val circle: Circle = Circle(x, y, max(size.toDouble(), 2.0)).also {
        it.stroke = color
        it.fill = color
    },
    val text: Text = Text(x + size + 3, y + 5, "").also {
        it.styleClass.add("mapPOIText")
    },
    override val zRange: ClosedRange<Double> = (-Double.MAX_VALUE..Double.MAX_VALUE)
) : MapNode, POI, Group(circle, text) {

    private var shown = 0
    private val names = name.split('|').filter { it.isNotBlank() }.distinct()

    override fun setShow(show: Boolean) {
        if (show) {
            isMouseTransparent = false
            circle.opacity = 1.0
            text.opacity = 1.0
        } else {
            isMouseTransparent = true
            circle.opacity = 0.0
            text.opacity = 0.0
        }
    }

    override fun setViewColor(color: Color) {
        text.fill = color
        circle.stroke = color.invert()
        circle.fill = color.invert()
    }

    override fun getViewColor(): Color? {
        return text.fill as? Color
    }

    init {
        styleClass.add("mapPOI")
        addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (it.button != MouseButton.PRIMARY) return@addEventHandler
            val index = (++shown) % names.size
            text.text = formatIfMultiText(names, index)
        }
        this.text.text = formatIfMultiText(names)
    }



    companion object {

        fun formatIfMultiText(names: List<String>, index: Int = 0): String {
            return if (names.size > 1) {
                "${names[index]} (${index + 1}/${names.size})"
            } else {
                names[0]
            }
        }

        fun buildFromString(vararg line: String): MapPOI2D {
            return MapPOI2D(
                line[0].toDouble(),
                line[1].toDouble(),
                Color.color(line[2].toDouble() / 255.0, line[3].toDouble() / 255.0, line[4].toDouble() / 255.0, 1.0),
                line[5].toInt(),
                line[6].replace('_', ' '),
            )
        }
    }
}
