package de.mknblch.eqmap.fx.marker

import javafx.animation.Transition
import javafx.scene.effect.MotionBlur
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.util.Duration

class PingMarker(val size: Int, color: Color, duration: Duration = Duration(200.0), count: Int = 1) : Circle() {

    init {
        opacity = 0.0
        stroke = color
        fill = Color.TRANSPARENT
        isMouseTransparent = true
        isPickOnBounds = false
        isSmooth = true
        isManaged = false
    }

    private val transition = object : Transition() {

        init {
            cycleDuration = duration
            cycleCount = count
        }

        override fun interpolate(frac: Double) {
            val v = 1.0 / frac - 1.0
            strokeWidth = v.coerceIn(1.0, 10.0)
            radius = (size * v).coerceIn(1.0, size.toDouble())
            opacity = if (v < 0.1) 0.0 else 0.9
        }
    }

    fun ping(x: Double, y: Double) {
        centerX = x
        centerY = y
        transition.playFromStart()
    }
}