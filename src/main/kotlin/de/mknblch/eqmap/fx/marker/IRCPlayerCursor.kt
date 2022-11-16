package de.mknblch.eqmap.fx.marker

import de.mknblch.eqmap.common.withAlpha
import javafx.animation.Transition
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Point2D
import javafx.scene.effect.Lighting
import javafx.scene.paint.Color
import javafx.util.Duration
import kotlin.math.sqrt

class IRCPlayerCursor(name: String) : CustomTextMarker(
    name = name,
    path = "M 149.996,0 C 67.157,0 0.001,67.161 0.001,149.997 0.001,232.833 67.157,300 149.996,300 232.835,300 299.999,232.837 " +
            "299.999,149.997 299.999,67.157 232.835,0 149.996,0 Z M 208.8,94.181 170.703,201.654 c -0.322,0.908 -1.118,1.561 " +
            "-2.067,1.699 -0.967,0.132 -1.904,-0.259 -2.469,-1.032 l -19.346,-26.434 -31.981,31.979 c -0.506,0.506 -1.17,0.76 " +
            "-1.834,0.76 -0.664,0 -1.328,-0.254 -1.834,-0.76 L 91.806,188.5 c -1.014,-1.014 -1.014,-2.653 0,-3.667 l 31.979,-31.984 " +
            "-26.436,-19.343 c -0.775,-0.568 -1.175,-1.517 -1.035,-2.469 0.14,-0.952 0.794,-1.748 1.699,-2.067 L 205.486,90.873 c 0.944,-0.335 " +
            "1.994,-0.099 2.7,0.609 0.71,0.704 0.949,1.755 0.614,2.699 z",
    135.0,
    0.075,
) {

    val colorProperty: SimpleObjectProperty<Color> = SimpleObjectProperty(Color.WHITE)

    init {
        svgPath.strokeWidth = 15.0
        svgPath.fillProperty().bind(colorProperty.map { it.withAlpha(0.05) })
        svgPath.strokeProperty().bind(colorProperty)
        text.strokeProperty().bind(colorProperty)
        text.fillProperty().bind(colorProperty.map { it.desaturate() })
    }

    fun setPos(position: Point2D) {
        super.moveTo(position.x, position.y)
        transition.playFromStart()
    }


    private val transition: Transition = object : Transition() {

        init {
            cycleDuration = Duration.seconds(60.0)
            cycleCount = 1
        }

        override fun interpolate(d: Double) {
            val f = (1.0 / d) - 1.0
            if (f <= 1.0 && f > 0.1) {
                this@IRCPlayerCursor.opacity = f
            } else if (f <= 0.1) {
                this@IRCPlayerCursor.opacity = 0.0
            } else {
                this@IRCPlayerCursor.opacity = 1.0
            }
        }
    }

}