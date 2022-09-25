package de.mknblch.eqmap

import de.mknblch.eqmap.config.ZoneMap
import de.mknblch.eqmap.map.Arrow
import de.mknblch.eqmap.map.MapLine
import de.mknblch.eqmap.map.MapObject
import de.mknblch.eqmap.map.MapPOI
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Border
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

@Component
class MapPane : ScrollPane() {

    private var cursor = Arrow(0.0, 0.0, 10.0, Color.BLUEVIOLET)
    private var mouseAnchorX: Double = 0.0
    private var mouseAnchorY: Double = 0.0
    private var initialTranslateX: Double = 0.0
    private var initialTranslateY: Double = 0.0
    private lateinit var map: ZoneMap
    private lateinit var group: Group
    private lateinit var enclosure: Pane
    private val strokeWidthProperty = SimpleDoubleProperty(1.0)

    val zViewDistance = SimpleDoubleProperty(35.0)
    val useZLayerViewDistance : SimpleBooleanProperty = SimpleBooleanProperty(true)
    val centerPlayerCursor : SimpleBooleanProperty = SimpleBooleanProperty(true)


    fun setMapContent(map: ZoneMap) {
        cursor.isVisible = false
        this.map = map
        content = prepare(map)
        showAllNodes()
        centerMap()
        zoomToBounds()
        layout()
    }

    fun centerMap() {
        val bounds = group.boundsInLocal
        val centerInLocal = Point2D(bounds.width / 2 + bounds.minX, bounds.height / 2 + bounds.minY)
        centerPoint(group.localToParent(centerInLocal))
    }

    fun manualSetZ(z: Double) {
        if (!this::map.isInitialized) return
        if (!useZLayerViewDistance.get()) return
        val minZ: Double = map.elements.minOf { it.zRange.start }
        val maxZ: Double = map.elements.maxOf { it.zRange.endInclusive }
        showZLayer(minZ + (maxZ - minZ) * z)
    }

    fun moveCursor(x: Double, y: Double, z: Double) {
        if (!this::group.isInitialized) return
        cursor.isVisible = true
        cursor.setPos(Point2D(x, y))
        if (centerPlayerCursor.get()) {
            centerPoint(group.localToParent(cursor.getPosition()))
        }
        if (useZLayerViewDistance.get()) showZLayer(z)
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
        map.elements.forEach {

            when (it) {
                is MapLine -> it.stroke = it.color.deriveColor(newColor.hue, newColor.saturation, newColor.brightness, newColor.opacity)
                is MapPOI -> it.text.fill = it.color.deriveColor(newColor.hue, newColor.saturation, newColor.brightness, newColor.opacity)
            }

        }
        cursor.fill = cursor.color.deriveColor(newColor.hue, newColor.saturation, newColor.brightness, newColor.opacity)
    }

    fun showAllNodes() {
        if (this::map.isInitialized) {
            map.elements.forEach(MapObject::show)
        }
    }

    private fun showZLayer(z: Double) {
        showZLayer((z - zViewDistance.get()..z + zViewDistance.get()))
    }

    private fun showZLayer(range: ClosedRange<Double>) {
        map.elements.forEach {
            if (it.inRangeTo(range)) {
                it.show()
            } else {
                it.hide()
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
            isFitToWidth = true

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
        cursor.strokeWidthProperty()?.bind(strokeWidthProperty)
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

    init {
        isPannable = false
        isFitToHeight = true // enables no-scrolling & dragging outside of map
        isFitToWidth = true
        hbarPolicy = ScrollBarPolicy.NEVER
        vbarPolicy = ScrollBarPolicy.NEVER
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapPane::class.java)
    }
}