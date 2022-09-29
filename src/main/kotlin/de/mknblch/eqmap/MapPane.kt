package de.mknblch.eqmap

import de.mknblch.eqmap.common.ColorTransformer
import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.config.ZoneMap
import de.mknblch.eqmap.map.Arrow
import de.mknblch.eqmap.map.MapLine
import de.mknblch.eqmap.map.MapPOI
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Border
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import java.awt.geom.Line2D
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@Lazy
@Component
class MapPane : StackPane() {

    private var cursor = Arrow(0.0, 0.0, 12.0, Color.BLUEVIOLET)
    private var mouseAnchorX: Double = 0.0
    private var mouseAnchorY: Double = 0.0
    private var initialTranslateX: Double = 0.0
    private var initialTranslateY: Double = 0.0
    private lateinit var map: ZoneMap
    private lateinit var group: Group
    private lateinit var enclosure: Pane

    private var colorTransformer: ColorTransformer = OriginalTransformer

    @Qualifier("zViewDistance")
    @Autowired
    private lateinit var zViewDistance: DoubleProperty

    @Qualifier("strokeWidthProperty")
    @Autowired
    private lateinit var strokeWidthProperty: DoubleProperty

    @Qualifier("useZLayerViewDistance")
    @Autowired
    private lateinit var useZLayerViewDistance: BooleanProperty

    @Qualifier("centerPlayerCursor")
    @Autowired
    private lateinit var centerPlayerCursor: BooleanProperty

    @Qualifier("showPoiProperty")
    @Autowired
    private lateinit var showPoiProperty: BooleanProperty

    fun setMapContent(map: ZoneMap) {
        cursor.isVisible = false
        this.map = map
        this.children.add(prepare(map))
        layout()
        showAllNodes()
        resetColor(colorTransformer)
        cursor.sizeProperty.bind(strokeWidthProperty)
        centerMap()
        zoomToBounds()
        layout()
        showPoiProperty.addListener { _, _, v ->
            map.elements.filterIsInstance<MapPOI>().forEach {
                it.setShow(v)
            }
        }
    }

    fun setColorTransformer(colorTransformer: ColorTransformer) {
        this.colorTransformer = colorTransformer
        resetColor(colorTransformer)
    }

    fun centerMap() {
        if (!this::map.isInitialized) return
        val bounds = group.boundsInLocal
        val centerInLocal = Point2D(bounds.width / 2 + bounds.minX, bounds.height / 2 + bounds.minY)
        centerPoint(group.localToParent(centerInLocal))
    }

    fun manualSetZ(z: Double) {
        if (!this::map.isInitialized) return
        if (!useZLayerViewDistance.get()) return
        val minZ: Double = map.elements.minOf { it.zRange.start }
        val maxZ: Double = map.elements.maxOf { it.zRange.endInclusive }
        drawZLayer(minZ + (maxZ - minZ) * z)
    }

    fun moveCursor(x: Double, y: Double, z: Double) {
        if (!this::group.isInitialized) return
        cursor.isVisible = true
        cursor.setPos(Point2D(x, y))
        if (centerPlayerCursor.get()) {
            centerPoint(group.localToParent(cursor.getPosition()))
        }
        if (useZLayerViewDistance.get()) drawZLayer(z)
    }

    fun moveCursorClick(target: Point2D) {
        if (!this::group.isInitialized) return
        cursor.isVisible = true
        group.parentToLocal(target).also(cursor::setPos) // TODO
        if (centerPlayerCursor.get()) {
            centerPoint(group.localToParent(cursor.getPosition()))
        }
    }

    fun deriveColor(newColor: Color) {
        if (!this::map.isInitialized) return
        resetColor(colorTransformer)
        map.elements.forEach {
            when (it) {
                is MapLine -> it.stroke = (it.stroke as Color).deriveColor(
                    newColor.hue,
                    newColor.saturation,
                    newColor.brightness,
                    newColor.opacity
                )
                is MapPOI -> it.text.fill = (it.text.fill as Color).deriveColor(
                    newColor.hue,
                    newColor.saturation,
                    newColor.brightness,
                    newColor.opacity
                )
            }

        }
        cursor.fill = cursor.color.deriveColor(newColor.hue, newColor.saturation, newColor.brightness, newColor.opacity)
    }

    fun resetColor(colorTransformer: ColorTransformer) {
        if (!this::map.isInitialized) return
        colorTransformer.apply(map.elements)
    }

    fun showAllNodes() {
        if (this::map.isInitialized) {
            map.elements.forEach {
                when (it) {
                    is MapLine -> it.setShow(true)
                    is MapPOI -> it.setShow(showPoiProperty.get())
                }
            }
        }
    }

    private fun drawZLayer(z: Double) {
        drawZLayer((z - zViewDistance.get()..z + zViewDistance.get()))
    }

    private fun drawZLayer(range: ClosedRange<Double>) {
        if (showPoiProperty.get()) {
            map.elements.forEach {
                it.setShow(it.inRangeTo(range))
            }
        } else {
            map.elements.filterIsInstance<MapLine>().forEach {
                it.setShow(it.inRangeTo(range))
            }
        }
    }

    private fun centerPoint(point: Point2D) {
        group.translateX += (enclosure.width / 2) - point.x
        group.translateY += (enclosure.height / 2) - point.y
    }

    private fun translateTo(point: Point2D, target: Point2D) {
        group.translateX += target.x - point.x
        group.translateY += target.y - point.y
    }

    private fun prepare(map: ZoneMap): Node {
        // unload
        unloadCurrent()
        // group map & cursor
        group = Group(*map.toTypedArray(), cursor)

        // register properties on elements
        registerNodeProperties()
        // background for moving and scaling
        enclosure = Pane(group)
        with(enclosure) {
            // properties
            border = Border.EMPTY

            // pressed handler
            addEventFilter(MouseEvent.MOUSE_PRESSED) { mouseEvent ->
                onMousePressed(mouseEvent)
            }
            // dragged handler
            addEventFilter(MouseEvent.MOUSE_DRAGGED) { mouseEvent ->
                onDrag(mouseEvent)
            }
            // click handler
            addEventFilter(MouseEvent.MOUSE_CLICKED) { mouseEvent ->
                onClick(mouseEvent)
            }
            // scroll handler
            addEventFilter(ScrollEvent.SCROLL) { mouseEvent ->
                onScroll(sign(mouseEvent.deltaY), Point2D(mouseEvent.x, mouseEvent.y))
                mouseEvent.consume()
            }
        }
        return enclosure
    }

    private fun registerNodeProperties() {
        map.elements.forEach { node ->
            // increase stroke width when zooming out
            (node as? Line)?.strokeWidthProperty()?.bind(strokeWidthProperty)
        }
    }

    private fun unloadCurrent() {
        if (this::group.isInitialized) {
            group.children.clear()
        }
        if (this::enclosure.isInitialized) {
            enclosure.children.clear()
        }
    }

    private fun onClick(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseButton.SECONDARY) {
            moveCursorClick(Point2D(mouseEvent.x, mouseEvent.y))
            mouseEvent.consume()
        }
    }

    private fun onMousePressed(mouseEvent: MouseEvent) {
        mouseAnchorX = mouseEvent.x
        mouseAnchorY = mouseEvent.y
        initialTranslateX = group.translateX
        initialTranslateY = group.translateY
        mouseEvent.consume()
    }

    private fun onDrag(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseButton.PRIMARY) {
            group.translateX = initialTranslateX + mouseEvent.x - mouseAnchorX
            group.translateY = initialTranslateY + mouseEvent.y - mouseAnchorY
            mouseEvent.consume()
        }
    }

    private fun onScroll(wheelDelta: Double, mousePoint: Point2D) {
        val v = wheelDelta * 0.05
        val localBeforeScroll = group.parentToLocal(mousePoint)
        // adapt scale
        val f = min(
            width / group.boundsInLocal.width,
            height / group.boundsInLocal.height
        ) * 0.75
        group.scaleX = max(f, group.scaleX + (group.scaleX * v))
        group.scaleY = max(f, group.scaleY + (group.scaleY * v))
        val clickInParent = group.localToParent(localBeforeScroll)
        // center target point at mouse pointer
        translateTo(clickInParent, mousePoint)
        // adapt stroke width when zooming
        strokeWidthProperty.set((1.0 / group.scaleY))
    }

    fun zoomToBounds() {
        layout()
        val f = min(
            width / group.boundsInLocal.width,
            height / group.boundsInLocal.height
        ) * 0.95
        group.scaleX = f
        group.scaleY = f
        strokeWidthProperty.set((1.0 / group.scaleY))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapPane::class.java)
    }
}