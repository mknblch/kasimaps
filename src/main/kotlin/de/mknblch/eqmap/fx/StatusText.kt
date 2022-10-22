package de.mknblch.eqmap.fx

import javafx.animation.FadeTransition
import javafx.scene.control.Label
import javafx.util.Duration

class StatusText: Label("") {

    init {
        styleClass.add("statusText")
        isMouseTransparent = true
    }

    private val fadeOut = FadeTransition(Duration(1000.0), this).also {
        it.fromValue = 1.0
        it.toValue = 0.0
    }

    fun setStatusText(text: String) {
        this.text = text
        fadeOut.playFromStart()
    }
}