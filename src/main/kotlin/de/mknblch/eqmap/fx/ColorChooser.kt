package de.mknblch.eqmap.fx

import de.mknblch.eqmap.common.ColorTransformer.Companion.generateMonochromePalette
import de.mknblch.eqmap.common.ColorTransformer.Companion.generatePalette
import javafx.beans.NamedArg
import javafx.beans.property.*
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import org.slf4j.LoggerFactory
import kotlin.math.*

internal open class ColorChooser @JvmOverloads constructor(
    colors: List<Color> = generatePalette(24, offset = 180.0) + generateMonochromePalette(6),
    @NamedArg("nColumns") nColumns: Double = 6.0 //calculateColumnSize(colors.size)
) : GridPane() {

    private val nRows: Double

    /**
     * The color the user has selected or the default initial color (the first color in the palette)
     */
    val chosenColor = SimpleObjectProperty<Color>(Color.BLACK)

    /**
     * Preferred size for a web palette tile
     */
    private val prefTileSize: DoubleProperty = SimpleDoubleProperty(MIN_TILE_SIZE)

    init {

        // create a color swatch.
        isSnapToPixel = false

        // calculate the number of columns and rows based on the number of colors and a golden ratio for layout.
        nRows = ceil(colors.size / nColumns)

        // create a bunch of button controls for color selection.
        for ((i, color) in colors.withIndex()) {
            val colorHex = "#" + color.toString().removePrefix("0x")
            // create a button for choosing a color.
            val colorChoice = Button()
            colorChoice.userData = color
            // position the button in the grid.
            setRowIndex(colorChoice, i / nColumns.toInt())
            setColumnIndex(colorChoice, i % nColumns.toInt())
            colorChoice.setMinSize(MIN_TILE_SIZE, MIN_TILE_SIZE)
            colorChoice.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE)
            // add a mouseover tooltip to display more info on the colour being examined.
            val graphic = Rectangle(30.0, 30.0, Color.web(colorHex))
            graphic.widthProperty().bind(prefTileSize.multiply(1.5))
            graphic.heightProperty().bind(prefTileSize.multiply(1.5))

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
            colorChoice.setOnMousePressed {
                (colorChoice.userData as? Color)?.also {
                    chosenColor.set(it)
                }
            }

            // add the color choice to the selection.
            children.add(colorChoice)
        }

//        (swatch.children[0] as Button).fire()
        style = "-fx-background-color: transparent; -fx-font-size: 16;"
        layoutBoundsProperty()
            .addListener { _, _, newBounds: Bounds ->
                prefTileSize.set(
                    max(MIN_TILE_SIZE, min(newBounds.width / nColumns, newBounds.height / nRows))
                )
                for (child in childrenUnmodifiable) {
                    val tile = child as Control
                    val margin = 0.0 // prefTileSize.get() / 10
                    tile.setPrefSize(prefTileSize.get() - 2 * margin, prefTileSize.get() - 2 * margin)
                    setMargin(child, Insets(margin))
                }
            }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ColorChooser::class.java)

        private const val GOLDEN_RATIO = 1.618
        private const val MIN_TILE_SIZE = 3.0

        fun calculateColumnSize(elements: Int): Double {
            return floor(sqrt(elements.toDouble()) * 2 / GOLDEN_RATIO)
        }
    }
}