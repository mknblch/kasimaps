package de.mknblch.eqmap.common

import de.mknblch.eqmap.common.ColorTransformer.Companion.generateMonochromePalette
import de.mknblch.eqmap.common.ColorTransformer.Companion.generatePalette
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal class BlackWhiteChooser(size: Int = 12) : ColorChooser(colors = generateMonochromePalette(size)) {

}