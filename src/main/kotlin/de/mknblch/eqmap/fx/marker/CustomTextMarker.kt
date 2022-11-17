package de.mknblch.eqmap.fx.marker

import javafx.scene.text.Text

open class CustomTextMarker(
    val initialTextValue: String,
    path: String,
    angle: Double = 0.0,
    scaling: Double = 1.0,
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    autoRotate: Boolean = true
) : CustomMarker(path, angle, scaling, xOffset, yOffset, autoRotate) {

    protected val text: Text = Text(initialTextValue)

    init {
        // add children
        this.children.add(text)
        text.translateX = 15.0 // TODO
        text.translateY = -10.0
//        text.isPickOnBounds = false
//        text.isManaged = false
    }

    fun setTextValue(textValue: String) {
        text.text = textValue
    }
}