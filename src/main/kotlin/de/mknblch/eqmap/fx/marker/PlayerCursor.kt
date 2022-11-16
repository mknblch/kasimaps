package de.mknblch.eqmap.fx.marker

import de.mknblch.eqmap.common.withAlpha
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Point2D
import javafx.scene.effect.Effect
import javafx.scene.effect.Glow
import javafx.scene.effect.Lighting
import javafx.scene.effect.MotionBlur
import javafx.scene.paint.Color

class PlayerCursor : CustomMarker(
    path = "M 470.172,490 235.086,339.229 1.4692383e-6,490 235.086,5.2441406e-7 Z",
    90.0,
    0.05,
) {

    val colorProperty: SimpleObjectProperty<Color> = SimpleObjectProperty(Color.DARKORANGE)

    init {
        svgPath.strokeWidth = 15.0
        svgPath.fillProperty().bind(colorProperty)
        svgPath.stroke = Color.BLACK
    }

    fun setPos(position: Point2D) {
        super.moveTo(position.x, position.y)
    }
}