package de.mknblch.eqmap.fx.marker

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Group
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.shape.SVGPath
import javafx.scene.text.Text
import javafx.scene.transform.Scale

class WaypointMarker(val path: SVGPath = SVGPath(), val text: Text = Text()) : Group(path, text) {

    private val scale: Scale

    val scaleProperty: SimpleDoubleProperty

    init {
        styleClass.add("waypoint")
        isVisible = false
        addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (it.button != MouseButton.SECONDARY) return@addEventHandler
            isVisible = false
        }
        //
        path.content = "M591.943,471a3.676,3.676,0,0,1-2.38-1.207C583,462.245,574,451.5,574,441a18," +
                "18,0,1,1,36,0c0,10.5-9,21.242-15.564,28.79A3.676,3.676,0,0,1,592.057,471ZM576,441c0," +
                "10.242,9.9,21.162,15.764,27.793a1.537,1.537,0,0,1,.206.211,1.767,1.767,0,0,0," +
                ".215-.155C598.1,462.162,608,451.242,608,441a16,16,0,0,0-32,0Zm7-.005a9,9,0,1,1,9," +
                "9A9.01,9.01,0,0,1,583,440.995Zm1,0a8,8,0,1,0,8-8A8.007,8.007,0,0,0,584,440.994Z"
        path.stroke = Color.GOLD
        path.translateX = -573.0 - path.boundsInLocal.width / 2
        path.translateY = -423.0 - path.boundsInLocal.height
        //
        text.fill = Color.GOLD
        text.translateY = -20.0
        text.translateX = 20.0
        //
        scale = Scale(path.translateX, path.translateY)
        scaleProperty = SimpleDoubleProperty().also {
            it.addListener { _, _, v ->
                scale.x = v.toDouble()
                scale.y = v.toDouble()
            }
        }
        transforms.add(scale)
    }

    fun setWaypoint(x: Double, y: Double, name: String) {
        text.text = name
        translateX = x
        translateY = y
        isVisible = true
    }

    fun reset() {
        text.text = ""
        translateX = 0.0
        translateY = 0.0
        isVisible = false
    }
}