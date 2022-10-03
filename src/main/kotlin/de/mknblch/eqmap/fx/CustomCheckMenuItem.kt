package de.mknblch.eqmap.fx

import javafx.beans.NamedArg
import javafx.scene.control.CheckBox
import javafx.scene.control.CustomMenuItem

class CustomCheckMenuItem(@NamedArg("text") text: String, @NamedArg("selected") selected: Boolean = true): CustomMenuItem(CheckBox(text)) {

    val checkbox = content as CheckBox

    fun selectedProperty() = checkbox.selectedProperty()

    init {
        styleClass.add("customCheckMenuItem")
        content.styleClass.add("customCheckBox")
        selectedProperty().set(selected)
    }
}