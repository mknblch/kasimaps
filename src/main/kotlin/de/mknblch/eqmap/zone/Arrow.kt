package de.mknblch.eqmap.zone

import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Point2D
import javafx.scene.effect.Lighting
import kotlin.jvm.JvmOverloads
import javafx.scene.paint.Color
import javafx.scene.shape.MoveTo
import javafx.scene.shape.LineTo
import javafx.scene.shape.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Arrow @JvmOverloads constructor(
    var x1: Double,
    var y1: Double,
    var size: Double = 10.0,
    val color: Color
) : Path() {

    private var x2: Double = x1
    private var y2: Double = y1 + size

    val sizeProperty: SimpleDoubleProperty = SimpleDoubleProperty(size)

    fun getPosition(): Point2D {
        return Point2D(x2, y2)
    }

    fun setPos(position: Point2D) {
        if (position.x == x2 && y2 == position.y) {
            return
        }
        x1 = x2
        y1 = y2
        x2 = position.x
        y2 = position.y
        draw()
    }

    private fun draw() {
        elements.clear()
        elements.add(MoveTo(x2, y2))
        val angle = atan2(y2 - y1, x2 - x1) - Math.PI / 2.0
        val sin = sin(angle)
        val cos = cos(angle)
        val s = sizeProperty.get().coerceAtLeast(1.0) * size
        val x1 = (-1.0 / 3 * cos + sqrt32 * sin) * s + x2
        val y1 = (-1.0 / 3 * sin - sqrt32 * cos) * s + y2
        val x2 = (1.0 / 3 * cos + sqrt32 * sin) * s + x2
        val y2 = (1.0 / 3 * sin - sqrt32 * cos) * s + y2
        elements.add(LineTo(x1, y1))
        elements.add(LineTo(x2, y2))
        elements.add(LineTo(this.x2, this.y2))
    }

    init {
        sizeProperty.addListener { _, _, _ ->
            draw()
        }
        fill = color
        stroke = Color.TRANSPARENT
        effect = Lighting()
    }
    companion object {
        private val sqrt32 = sqrt(3.0) / 2
    }
}