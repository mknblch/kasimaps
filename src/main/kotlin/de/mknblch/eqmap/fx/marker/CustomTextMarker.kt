package de.mknblch.eqmap.fx.marker

import javafx.scene.text.Text

open class CustomTextMarker(
    val name: String,
    path: String,
    angle: Double = 0.0,
    scaling: Double = 1.0,
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
) : CustomMarker(path, angle, scaling, xOffset, yOffset) {

    protected val text: Text = Text(name)

    init {
        // add children
        this.children.add(text)
        text.translateX = 15.0 // TODO
        text.translateY = -10.0
    }

}