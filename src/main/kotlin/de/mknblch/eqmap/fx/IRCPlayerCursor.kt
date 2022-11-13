package de.mknblch.eqmap.fx

import javafx.animation.Transition
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.effect.Lighting
import kotlin.jvm.JvmOverloads
import javafx.scene.paint.Color
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.text.Text
import javafx.util.Duration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class IRCPlayerCursor @JvmOverloads constructor(
    val name: String,
    var x: Double,
    var y: Double,
    var size: Double = 10.0,
    val color: Color,
    val path: Path = Path(),
    val text: Text = Text(10.0, 0.0, name )
) : Group(path, text) {

    protected var x2: Double = x
    protected var y2: Double = y + size
    private val fadeStart = 1.0
    private val fadeOut = 0.1
    val sizeProperty: SimpleDoubleProperty = SimpleDoubleProperty(size)
    val scaleProperty: SimpleDoubleProperty = SimpleDoubleProperty(1.0)

    init {
        isMouseTransparent = true
        isPickOnBounds = false

        sizeProperty.addListener { _, _, _ ->
            draw()
        }
        scaleProperty.addListener { _, _, _ ->
            draw()
        }
        path.fill = color
        path.stroke = Color.TRANSPARENT
        text.stroke = color
        text.fill = color
        effect = Lighting()
    }

    fun setPos(position: Point2D) {
        if (position.x == x2 && y2 == position.y) {
            return
        }
        x = x2
        y = y2
        x2 = position.x
        y2 = position.y
        draw()
        this.translateX = x2
        this.translateY = y2
        transition.playFromStart()
    }

    fun draw() {
        path.elements.clear()
        path.elements.add(MoveTo(0.0, 0.0))
        val angle = atan2(y2 - y, x2 - x) - Math.PI / 2.0
        val sin = sin(angle)
        val cos = cos(angle)
        val s = sizeProperty.get().coerceAtLeast(1.0) * size * scaleProperty.get()
        val x1 = (-1.0 / 3 * cos + sqrt32 * sin) * s
        val y1 = (-1.0 / 3 * sin - sqrt32 * cos) * s
        val x2 = (1.0 / 3 * cos + sqrt32 * sin) * s
        val y2 = (1.0 / 3 * sin - sqrt32 * cos) * s
        path.elements.add(LineTo(x1, y1))
        path.elements.add(LineTo(x2, y2))
        path.elements.add(LineTo(0.0, 0.0))
    }


    private val transition: Transition = object : Transition() {

        init {
            cycleDuration = Duration.seconds(60.0)
            cycleCount = 1
        }

        override fun interpolate(d: Double) {
            val f = (1.0 / d) - 1.0
            if (f <= fadeStart && f > fadeOut) {
                this@IRCPlayerCursor.opacity = f
            } else if (f <= fadeOut) {
                this@IRCPlayerCursor.opacity = 0.0
            } else {
                this@IRCPlayerCursor.opacity = 1.0
            }
        }
    }

    companion object {
        private val sqrt32 = sqrt(3.0) / 2
    }
}