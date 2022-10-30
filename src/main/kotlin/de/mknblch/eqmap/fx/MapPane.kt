package de.mknblch.eqmap.fx

import de.mknblch.eqmap.common.ColorTransformer
import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.withAlpha
import de.mknblch.eqmap.zone.*
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.event.EventTarget
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.*
import javafx.scene.layout.Background
import javafx.scene.layout.Border
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Text
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign


@Lazy
@Component
class MapPane : StackPane() {

    private lateinit var map: ZoneMap
    private lateinit var group: Group
    private lateinit var enclosure: Pane

    private var mouseAnchorX: Double = 0.0
    private var mouseAnchorY: Double = 0.0
    private var initialTranslateX: Double = 0.0
    private var initialTranslateY: Double = 0.0
    private val zOrdinate: DoubleProperty = SimpleDoubleProperty(0.0)
    private val locationPing = PingMarker(100, Color.web("#00CCFF"))
    private val copyPing = PingMarker(100, Color.SPRINGGREEN)
    private val waypoint = WaypointMarker()
    private var colorTransformer: ColorTransformer = OriginalTransformer
    private val cursorHint = Text(0.0, 0.0, "").also {
        it.isVisible = false
        it.styleClass.add("cursorHint")
    }
    private val statusLabel = StatusText()
    private val strokeWidthProperty = SimpleDoubleProperty(1.0)
    private val clipboard = Clipboard.getSystemClipboard()

    val cursor = Arrow(0.0, 0.0, 14.0, Color.WHITE)

    val zViewDistance: DoubleProperty = SimpleDoubleProperty(35.0)

    val alpha: DoubleProperty = SimpleDoubleProperty(1.0)

    val useZLayerViewDistance: SimpleMapProperty<String, Boolean> =
        SimpleMapProperty<String, Boolean>(FXCollections.observableHashMap())

    val centerPlayerCursor: BooleanProperty = SimpleBooleanProperty()

    val showPoiProperty: BooleanProperty = SimpleBooleanProperty()

    val pingOnMove: BooleanProperty = SimpleBooleanProperty()

    val falseColor: ObjectProperty<Color> = SimpleObjectProperty(Color.RED)

    val backgroundColor: ObjectProperty<Color> = SimpleObjectProperty(Color.BLACK)

    val cursorColor: ObjectProperty<Color> = SimpleObjectProperty(Color.WHITE)

    @PostConstruct
    fun init() {
        cursor.sizeProperty.bind(strokeWidthProperty)
        waypoint.scaleProperty.bind(strokeWidthProperty)
        cursorColor.addListener { _, _, v ->
            if (v != null) cursor.fill = v
        }
        cursor.fill = cursorColor.get()
        showPoiProperty.addListener { _, _, _ ->
            redraw()
        }
        backgroundColor.addListener { _, _, v ->
            if (v != null) setBackgroundColor(v)
        }
        falseColor.addListener { _, _, v ->
            if (v != null) deriveColor(v)
        }
        alpha.addListener { _, _, v ->
            setAlpha(v.toDouble())
            statusLabel.setStatusText("Alpha: ${(v.toDouble() * 100).roundToInt()}%")
        }
        useZLayerViewDistance.addListener { _, _, v ->
            redraw()
        }
    }

    fun setCursorColor(color: Color) {
        cursor.fill = color
    }

    fun setMapContent(map: ZoneMap) = synchronized(this) {
        logger.debug("loading zone ${map.name}")
        cursor.isVisible = false
        waypoint.reset()
        this.map = map
        children.clear()
        children.add(prepare(map))
        children.add(cursorHint)
        children.add(locationPing)
        children.add(copyPing)
        children.add(statusLabel)
        StackPane.setAlignment(statusLabel, Pos.BOTTOM_LEFT)
//        children.add(lockButton)
//        StackPane.setAlignment(lockButton, Pos.BOTTOM_RIGHT)
        layout()
        redraw()
        resetColor(colorTransformer)
        deriveColor(falseColor.get())
        centerMap()
        zoomToBounds()
        layout()
        setBackgroundColor(backgroundColor.get())
        setStatusText("load ${map.name.capitalize()}")
    }

    fun getMapShortName(): String? {
        if (!this::map.isInitialized) return null
        return map.shortName
    }

    fun setColorTransformer(colorTransformer: ColorTransformer) {
        logger.debug("applying ${colorTransformer::class.simpleName}")
        this.colorTransformer = colorTransformer
        resetColor(colorTransformer)
    }

    fun centerMap() {
        if (!this::map.isInitialized) return
        val bounds = group.boundsInLocal
        val centerInLocal = Point2D(bounds.width / 2 + bounds.minX, bounds.height / 2 + bounds.minY)
        centerPoint(group.localToParent(centerInLocal))
    }

    fun moveCursor(x: Double, y: Double, z: Double) {
        if (!this::group.isInitialized) return
        cursor.isVisible = true
        cursor.setPos(Point2D(x, y))
        if (centerPlayerCursor.get()) centerPoint(group.localToParent(cursor.getPosition()))
        if (useZLayerViewDistance[map.shortName] == true) drawZLayer(z)
        if (pingOnMove.get()) locationPing(Point2D(x, y))
    }

    fun deriveColor(newColor: Color) {
        if (!this::map.isInitialized) return
        logger.debug("deriving from color $newColor")
        resetColor(colorTransformer)
        map.elements.forEach { node ->
            node.getViewColor()?.also {
                node.setViewColor(
                    it.deriveColor(
                        newColor.hue,
                        newColor.saturation,
                        newColor.brightness,
                        newColor.opacity
                    )
                )
            }
        }
    }

    fun resetColor(colorTransformer: ColorTransformer) {
        if (!this::map.isInitialized) return
        colorTransformer.apply(map.elements)
    }

    private fun drawZLayer(z: Double) {
        zOrdinate.set(z)
        redraw()
    }

    fun redraw() {
        if (!this::map.isInitialized) return
        map.layer.forEach { layer ->
            redrawLayer(layer)
        }
    }

    private fun redrawLayer(layer: MapLayer) {
        layer.nodes.forEach { node ->
            // hide if whole layer is set to be hidden
            if (!layer.show) {
                node.setShow(false)
                return@forEach
            }
            // hide POI if switched off
            if ((node is MapPOI3D || node is MapPOI2D) && !showPoiProperty.get()) {
                node.setShow(false)
                return@forEach
            }
            // hide if not in z-range and switch is on
            if (useZLayerViewDistance[map.shortName] == true) {
                val range = (zOrdinate.get() - zViewDistance.get())..(zOrdinate.get() + zViewDistance.get())
                if (!node.inRangeTo(range)) {
                    node.setShow(false)
                    return@forEach
                }
            }
            // show otherwise
            node.setShow(true)
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
        group = Group(*map.toTypedArray(), cursor, waypoint)

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
            addEventFilter(MouseEvent.MOUSE_RELEASED) { mouseEvent ->
                onMouseReleased(mouseEvent)
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
                onScroll(mouseEvent)
            }
            // move handler
            addEventFilter(MouseEvent.MOUSE_MOVED) { mouseEvent ->
                onMouseMoved(mouseEvent)
            }
        }
        return enclosure
    }

    private fun onMouseMoved(mouseEvent: MouseEvent) {
        val b = group.parentToLocal(Point2D(mouseEvent.x, mouseEvent.y))
        cursorHint.text = "( ${-b.y.roundToInt()} x ${-b.x.roundToInt()} )"
        setCursorHintPosition(mouseEvent)
    }

    private fun registerNodeProperties() {
        map.elements.forEach { node ->
            // increase stroke width when zooming out
            (node as? Line)?.strokeWidthProperty()?.bind(strokeWidthProperty)
        }
    }

    fun setAlpha(alpha: Double) {
        if (!this::map.isInitialized) return
        statusLabel.setStatusText("Alpha: ${(alpha * 100).roundToInt()}%")
        logger.debug("setting alpha to $alpha")
        background = Background.fill(backgroundColor.get().withAlpha(alpha))
    }

    fun setBackgroundColor(color: Color) {
        if (!this::map.isInitialized) return
        val withAlpha = color.withAlpha(alpha.get())
        logger.debug("setting background color to $withAlpha")
        background = Background.fill(withAlpha)
        val inverted = color.invert()
        setCursorHintColor(inverted)
        map.elements.forEach { node ->
            // increase stroke width when zooming out
            (node as? MapPOI3D)?.text?.stroke = inverted
            (node as? MapPOI2D)?.text?.stroke = inverted
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
            val local = group.parentToLocal(Point2D(mouseEvent.x, mouseEvent.y))
            val parent = mouseEvent.pickResult.intersectedNode.parent
            if (parent is WaypointMarker || parent is POI) return
            showCopyPing(local)
            clipboard.setContent(ClipboardContent().also {
                val formatPing = formatPing(local, mouseEvent.target)
                logger.debug("setting clipboard text: $formatPing")
                it[DataFormat.PLAIN_TEXT] = formatPing
                statusLabel.setStatusText("Location copied to clipboard")
            })
        }
    }

    private fun formatPing(local: Point2D, target: EventTarget): String {
        return "!ping ${map.shortName}, ${-local.y.roundToInt()}, ${-local.x.roundToInt()}"
    }

    fun locationPing(point: Point2D, centerAtPing: Boolean = false) {
        val parent = group.localToParent(point)
        locationPing.ping(parent.x, parent.y)
        if (centerAtPing) {
            centerPoint(parent)
        }
    }

    fun showCopyPing(point: Point2D) {
        val parent = group.localToParent(point)
        copyPing.ping(parent.x, parent.y)
    }

    private fun onMouseReleased(mouseEvent: MouseEvent) {
        setCursorHintOpaque(true)
    }

    private fun onMousePressed(mouseEvent: MouseEvent) {
        mouseAnchorX = mouseEvent.x
        mouseAnchorY = mouseEvent.y
        initialTranslateX = group.translateX
        initialTranslateY = group.translateY
        setCursorHintOpaque(false)
    }

    private fun onDrag(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseButton.PRIMARY) {
            group.translateX = initialTranslateX + mouseEvent.x - mouseAnchorX
            group.translateY = initialTranslateY + mouseEvent.y - mouseAnchorY
            setCursorHintPosition(mouseEvent)
        }
    }

    private fun setCursorHintPosition(mouseEvent: MouseEvent) {
        cursorHint.translateX = mouseEvent.x + 15
        cursorHint.translateY = mouseEvent.y + 15
    }

    private fun onScroll(event: ScrollEvent) {
        val wheelDelta: Double = sign(event.deltaY)
        val v = wheelDelta * 0.05
        if (event.isControlDown) {
            changeZAxis(wheelDelta)
        } else {
            zoomAt(Point2D(event.x, event.y), v)
            // adapt stroke width when zooming
            strokeWidthProperty.set((1.0 / group.scaleY))
        }
    }

    private fun changeZAxis(v: Double) {
        zOrdinate.set(zOrdinate.get() + v * 10)
        redraw()
        statusLabel.setStatusText("Z: ${zOrdinate.get().roundToInt()}")
    }

    private fun zoomAt(mousePoint: Point2D, v: Double) {
        val localBeforeScroll = group.parentToLocal(mousePoint)
        // adapt scale
        val f = min(
            width / group.boundsInLocal.width,
            height / group.boundsInLocal.height
        ) * 0.75
        val value = max(f, group.scaleY + (group.scaleY * v))
        statusLabel.setStatusText("Zoom: ${(value * 100).toInt()}%")
        logger.debug("zooming into $localBeforeScroll by ${(value * 100).toInt()}%")
        group.scaleX = value
        group.scaleY = value
        val clickInParent = group.localToParent(localBeforeScroll)
        // center target point at mouse pointer
        translateTo(clickInParent, mousePoint)
    }

    fun zoomToBounds() {
        val f = min(
            width / group.boundsInLocal.width,
            height / group.boundsInLocal.height
        ) * 0.90
        group.scaleX = f
        group.scaleY = f
        strokeWidthProperty.set((1.0 / group.scaleY))
    }

    fun setCursorTextVisible(v: Boolean) {
        cursorHint.isVisible = v
    }

    fun setCursorHintColor(color: Color) {
        cursorHint.stroke = color
    }

    fun setCursorHintOpaque(visible: Boolean) {
        cursorHint.opacity = if (visible) 1.0 else 0.0
    }

    fun userPing(x: Double, y: Double, from: String) {
        if (!this::map.isInitialized) return
        if (!map.pointInBounds(x, y)) return
        logger.debug("setting waypoint to $x, $y")
        waypoint.setWaypoint(x, y, from)
        centerPoint(group.localToParent(Point2D(x, y)))
        statusLabel.setStatusText("new waypoint from $from")
    }

    fun resetWaypoint() {
        waypoint.reset()
    }

    fun setStatusText(text: String) {
        statusLabel.setStatusText(text)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapPane::class.java)
    }
}