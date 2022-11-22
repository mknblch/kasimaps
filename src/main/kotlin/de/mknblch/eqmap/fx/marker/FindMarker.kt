package de.mknblch.eqmap.fx.marker

import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.shape.FillRule

open class FindMarker() : CustomTextMarker(
    "",
    "M591.943,471a3.676,3.676,0,0,1-2.38-1.207C583,462.245,574,451.5,574,441a18," +
            "18,0,1,1,36,0c0,10.5-9,21.242-15.564,28.79A3.676,3.676,0,0,1,592.057,471ZM576,441c0," +
            "10.242,9.9,21.162,15.764,27.793a1.537,1.537,0,0,1,.206.211,1.767,1.767,0,0,0," +
            ".215-.155C598.1,462.162,608,451.242,608,441a16,16,0,0,0-32,0Zm7-.005a9,9,0,1,1,9," +
            "9A9.01,9.01,0,0,1,583,440.995Zm1,0a8,8,0,1,0,8-8A8.007,8.007,0,0,0,584,440.994Z",
    0.0,
    1.0,
    -573.0,
    -443.0,
    autoRotate = false
) {


    init {
        svgPath.styleClass.add("findMarker")
        text.styleClass.add("findMarker")
        isVisible = false
        addEventHandler(MouseEvent.MOUSE_CLICKED) {
            if (it.button == MouseButton.PRIMARY) {
                // left click hide
                isVisible = false
                opacity = 0.0
                isMouseTransparent = true
            }
            // right click remove all
            if (it.button == MouseButton.SECONDARY) onRemoveClick(it)
        }
        isMouseTransparent = false
        //
        text.translateY = -20.0
        text.translateX = 25.0
        svgPath.fillRule = FillRule.EVEN_ODD

        //
    }

    open fun onRemoveClick(mouseEvent: MouseEvent) {
    }

    fun setWaypoint(x: Double, y: Double, name: String) {
        setTextValue(name)
        moveTo(x, y)
        opacity = 1.0
        isMouseTransparent = false
    }
}