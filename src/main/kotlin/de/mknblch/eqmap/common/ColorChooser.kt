package de.mknblch.eqmap.common

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
import kotlin.math.max
import kotlin.math.min

internal class ColorChooser @JvmOverloads constructor(colors: List<Color> = generatePalette(30, offset = 190.0)) : VBox() {
    private val GOLDEN_RATIO = 1.618
    private val MIN_TILE_SIZE = 3.0
    private val nColumns: Double
    private val nRows: Double

    /**
     * The color the user has selected or the default initial color (the first color in the palette)
     */
    private val chosenColor = ReadOnlyObjectWrapper<Color>()
    fun getChosenColor(): Color {
        return chosenColor.get()
    }

    fun chosenColorProperty(): ReadOnlyObjectProperty<Color> {
        return chosenColor.readOnlyProperty
    }

    /**
     * Preferred size for a web palette tile
     */
    private val prefTileSize: DoubleProperty = SimpleDoubleProperty(MIN_TILE_SIZE)


    init {

        // create a pane for showing info on the chosen color.
        val colorInfo = HBox()

        // create a color swatch.
        val swatch = GridPane()
        swatch.isSnapToPixel = false

        // calculate the number of columns and rows based on the number of colors and a golden ratio for layout.
        nColumns = Math.floor(Math.sqrt(colors.size.toDouble()) * 2 / GOLDEN_RATIO)
        nRows = Math.ceil(colors.size / nColumns)

        // create a bunch of button controls for color selection.
        var i = 0
        for (color in colors) {
            val colorHex = "#" + color.toString().removePrefix("0x")

            // create a button for choosing a color.
            val colorChoice = Button()
            colorChoice.userData = color


            // position the button in the grid.
            GridPane.setRowIndex(colorChoice, i / nColumns.toInt())
            GridPane.setColumnIndex(colorChoice, i % nColumns.toInt())
            colorChoice.setMinSize(MIN_TILE_SIZE, MIN_TILE_SIZE)
            colorChoice.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)

            // add a mouseover tooltip to display more info on the colour being examined.
            // todo it would be nice to be able to have the tooltip appear immediately on mouseover, but there is no easy way to do this, (file jira feature request?)
            val graphic = Rectangle(30.0, 30.0, Color.web(colorHex))
            graphic.widthProperty().bind(prefTileSize) //.multiply(1.5))
            graphic.heightProperty().bind(prefTileSize) //.multiply(1.5))

            // color the button appropriately and change it's hover functionality (doing some of this in a css sheet would be better).
            val backgroundStyle = "-fx-background-color: $colorHex; -fx-background-insets: 0; -fx-background-radius: 1;"
            colorChoice.style = backgroundStyle
            colorChoice.onMouseEntered = EventHandler {
                val borderStyle =
                    "-fx-border-color: ladder($colorHex, whitesmoke 49%, darkslategrey 50%); -fx-border-width: 1;"
                colorChoice.style = backgroundStyle + borderStyle
            }
            colorChoice.onMouseExited = EventHandler {
                val borderStyle = "-fx-border-width: 0; -fx-border-insets: 1;"
                colorChoice.style = backgroundStyle + borderStyle
            }

            // choose the color when the button is clicked.
            colorChoice.onAction = EventHandler { chosenColor.set(colorChoice.userData as Color) }

            // add the color choice to the swatch selection.
            swatch.children.add(colorChoice)
            i++
        }

        // select the first color in the chooser.
        (swatch.children[0] as Button).fire()

        // layout the color picker.
        children.addAll(swatch, colorInfo)
        setVgrow(swatch, Priority.ALWAYS)
        style = "-fx-background-color: transparent; -fx-font-size: 16;"
        swatch.layoutBoundsProperty()
            .addListener { _, _, newBounds: Bounds ->
                prefTileSize.set(
                    max(MIN_TILE_SIZE, min(newBounds.width / nColumns, newBounds.height / nRows))
                )
                for (child in swatch.childrenUnmodifiable) {
                    val tile = child as Control
                    val margin = 0.0 // prefTileSize.get() / 10
                    tile.setPrefSize(prefTileSize.get() - 2 * margin, prefTileSize.get() - 2 * margin)
                    GridPane.setMargin(child, Insets(margin))
                }
            }
    }
}