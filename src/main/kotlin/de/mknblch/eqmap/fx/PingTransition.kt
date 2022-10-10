package de.mknblch.eqmap.fx

import javafx.animation.Transition
import javafx.scene.shape.Circle
import javafx.util.Duration

class PingTransition(val size: Int, val circle: Circle): Transition(200.0) {

    init {
        cycleDuration = Duration(250.0)
        cycleCount = 2
    }

    override fun interpolate(frac: Double) {
        val v = 1.0 / frac - 1.0
        circle.radius = (size * v).coerceIn(0.0, 100.0)
        circle.opacity = v.coerceAtLeast(0.0)
        println("$v ${(1.0 / frac)}")
    }
}