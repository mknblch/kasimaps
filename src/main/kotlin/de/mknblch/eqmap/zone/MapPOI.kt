package de.mknblch.eqmap.zone

import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text
import kotlin.math.absoluteValue

private const val POI_SIZE = 3.0

data class MapPOI(
    val names: List<String>,
    val x: Double,
    val y: Double,
    val z: Double? = null,
    override val color: Color,
    val size: Double = POI_SIZE
) : MapNode, Group() {


    override val zRange: ClosedRange<Double> = if (z == null) {
        (-Double.MAX_VALUE .. Double.MAX_VALUE)
    } else {
        (z..z)
    }

    private val circle: Circle = Circle(x, y, size).also {
        it.stroke = color
        it.fill = color
    }
    val text: Text = Text(x + size + 3, y + 5, "")
    private var clicks: Int = 0

    init {
        this.children.addAll(circle, text)
        addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (it.button != MouseButton.PRIMARY) return@addEventHandler
            val index = (++clicks) % names.size
            text.text = formatIfMultiText(names, index)
        }
        this.text.text = formatIfMultiText(names)
        styleClass.add("mapPOI")
    }

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

    override fun getViewColor(): Color? {
        return text.fill as? Color
    }

    override fun setViewColor(color: Color) {
        text.fill = color
        circle.stroke = color.invert()
        circle.fill = color.invert()
    }

    companion object {

        fun formatIfMultiText(names: List<String>, index: Int = 0): String {
            return if (names.size > 1) {
                "${names[index]} (${index + 1}/${names.size})"
            } else {
                names[0]
            }
        }

        fun build2DFromString(vararg line: String): MapPOI {

            val x = line[0].toDouble()
            val y = line[1].toDouble()
            val r = line[2].toDouble() / 255.0
            val g = line[3].toDouble() / 255.0
            val b = line[4].toDouble() / 255.0
            val size = line[5].toDouble()
            val names = line[6].replace('_', ' ').split('|')
            return MapPOI(
                names = names,
                x = x,
                y = y,
                z = null,
                color = Color(r, g, b, 1.0),
                size = size
            )
        }

        fun build3DFromString(vararg line: String): MapPOI {

            val x = line[0].toDouble()
            val y = line[1].toDouble()
            val z = line[2].toDouble()
            val r = line[3].toDouble() / 255.0
            val g = line[4].toDouble() / 255.0
            val b = line[5].toDouble() / 255.0
            val size = line[6].toDouble()
            val names = line[7].replace('_', ' ').split('|')

            return MapPOI(
                names = names,
                x = x,
                y = y,
                z = z,
                color = Color(r, g, b, 1.0),
                size = size
            )
        }
    }

    fun inRangeTo(otherZ: Double, maxRange: Double) : Boolean {
        // empty z is always in range
        return z?.let { (it - otherZ).absoluteValue <= maxRange } ?: true
    }

}
