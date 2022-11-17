package de.mknblch.eqmap.fx.marker

import javafx.beans.binding.Bindings.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Group
import javafx.scene.shape.FillRule
import javafx.scene.shape.SVGPath
import javafx.scene.transform.Scale
import kotlin.math.atan2

open class CustomMarker(
    path: String,
    private val angle: Double = 0.0,
    scaling: Double = 1.0,
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    val autoRotate: Boolean = true
) : Group() {

    val scaleProperty: SimpleDoubleProperty = SimpleDoubleProperty(1.0)
    val zoomProperty: SimpleDoubleProperty = SimpleDoubleProperty(1.0)

    private val intermediateScale = multiply(scaleProperty, zoomProperty)

    protected val svgPath: SVGPath = SVGPath()

    private var x0: Double = 0.0
    private var y0: Double = 0.0

    init {
        // add children
        this.children.add(svgPath)
        // default invisible
        isVisible = false
        // container properties
//        isFocusTraversable = false
        isMouseTransparent = true
//        isPickOnBounds = false
//        isManaged = false
        svgPath.isMouseTransparent = true
//        svgPath.isManaged = false
        // create path
        svgPath.content = path
        svgPath.fillRule = FillRule.NON_ZERO
        // center
        svgPath.translateX = xOffset - svgPath.boundsInLocal.width / 2
        svgPath.translateY = yOffset - svgPath.boundsInLocal.height / 2
        // adapt angle
        svgPath.rotate = angle
        // scale
        svgPath.scaleX = scaling
        svgPath.scaleY = scaling
        // scale property
        registerScaleProperty()
    }

    private fun registerScaleProperty() {
        val scale = Scale(svgPath.translateX, svgPath.translateY)
        intermediateScale.addListener { _, _, v ->
            val d = v.toDouble()//.coerceAtLeast(1.0)
            scale.x = d
            scale.y = d
        }
        transforms.add(scale)
    }

    fun moveTo(x: Double, y: Double) {
        isVisible = true
        if (x == x0 && y == y0) return
        if (autoRotate) svgPath.rotate = atan2(y0 - y, x0 - x) * 180.0 / Math.PI - angle
        translateX = x
        translateY = y
        x0 = x
        y0 = y
    }

}