package de.mknblch.eqmap

import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.*
import de.mknblch.eqmap.fx.BlackWhiteChooser
import de.mknblch.eqmap.fx.ColorChooser
import de.mknblch.eqmap.fx.CustomCheckMenuItem
import de.mknblch.eqmap.fx.MapPane
import de.mknblch.eqmap.zone.LayerComparator
import de.mknblch.eqmap.zone.ZoneMap
import javafx.application.Platform
import javafx.beans.property.*
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.util.*
import javax.annotation.PreDestroy
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


@Component
@FxmlResource("fxml/Map.fxml")
class MapController : Initializable {
    @Autowired
    private lateinit var zones: List<ZoneMap>

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Autowired
    private lateinit var properties: PersistentProperties

    @FXML
    private lateinit var parentPane: StackPane

    @FXML
    lateinit var mapPane: MapPane

    @FXML
    lateinit var resetMenuItem: MenuItem

    @FXML
    private lateinit var zoneMenu: Menu

    @FXML
    private lateinit var poiLayerMenu: Menu

    @FXML
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var colorChooser: ColorChooser

    @FXML
    private lateinit var cursorColorChooser: ColorChooser

    @FXML
    private lateinit var blackWhiteChooser: BlackWhiteChooser

    @FXML
    private lateinit var showPoi: CustomCheckMenuItem

    @FXML
    private lateinit var showCursorText: CustomCheckMenuItem

    @FXML
    private lateinit var centerCheckMenuItem: CustomCheckMenuItem

    @FXML
    private lateinit var zLayerCheckMenuItem: CustomCheckMenuItem

    @FXML
    lateinit var lockWindowMenuItem: CustomCheckMenuItem

    @FXML
    private lateinit var transparentWindow: CustomCheckMenuItem

    @FXML
    private lateinit var pingOnMoveCheckMenuItem: CustomCheckMenuItem

    @FXML
    private lateinit var enableWaypoint: CustomCheckMenuItem

    @FXML
    private lateinit var zRadio: RadioMenuItem

    @FXML
    private lateinit var originalRadio: RadioMenuItem

    @FXML
    private lateinit var cursorScaleSlider: Slider

    @Qualifier("useZLayerViewDistance")
    @Autowired
    private lateinit var useZLayerViewDistance: SimpleMapProperty<String, Boolean>

    @Qualifier("centerPlayerCursor")
    @Autowired
    private lateinit var centerPlayerCursor: SimpleBooleanProperty

    @Qualifier("showPoiProperty")
    @Autowired
    private lateinit var showPoiProperty: SimpleBooleanProperty

    @Qualifier("transparency")
    @Autowired
    private lateinit var transparency: SimpleDoubleProperty

    @Qualifier("falseColor")
    @Autowired
    private lateinit var falseColor: ObjectProperty<Color>

    @Qualifier("backgroundColor")
    @Autowired
    private lateinit var backgroundColor: ObjectProperty<Color>

    @Qualifier("cursorColor")
    @Autowired
    private lateinit var cursorColor: ObjectProperty<Color>

    @Qualifier("pingOnMove")
    @Autowired
    private lateinit var pingOnMove: BooleanProperty

    private var xOffset: Double = 0.0
    private var yOffset: Double = 0.0


    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        populateZoneMenu()
        mapPane.background = Background.EMPTY

        // get ui state
        centerCheckMenuItem.selectedProperty().set(properties.getOrSet("centerPlayerCursor", true))
        centerCheckMenuItem.selectedProperty().addListener { _, _, v ->
            properties.set("centerPlayerCursor", v)
        }
//        // zlayer
//
        zLayerCheckMenuItem.selectedProperty().addListener { _, _, v: Boolean ->
            mapPane.getMapShortName()?.also {
                properties.getMap<Boolean>("useZLayerViewDistance")[it] = v
                useZLayerViewDistance[it] = v
                mapPane.redraw()
            }
        }

        showPoi.selectedProperty().set(properties.getOrSet("showPoi", true))
        showPoi.selectedProperty().addListener { _, _, v ->
            properties.set("showPoi", v)
        }

        pingOnMoveCheckMenuItem.selectedProperty().set(properties.getOrSet("pingOnMove", true))
        pingOnMove.bind(pingOnMoveCheckMenuItem.selectedProperty())
        pingOnMove.addListener { _, _,v ->
            properties.set("pingOnMove", v)
        }
        // register properties
        centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
//        useZLayerViewDistance.bind(zLayerCheckMenuItem.selectedProperty())
        showPoiProperty.bind(showPoi.selectedProperty())
        //
        useZLayerViewDistance.addListener { _, _, _ ->
            mapPane.redraw()
        }
        val primaryStage: Stage = context.getBean("primaryStage") as Stage
        // transparency settings
        transparentWindow.selectedProperty().set(properties.getOrSet("useTransparency", false))
        transparentWindow.selectedProperty().addListener { _, _, newValue ->
            primaryStage.opacity = if (newValue) transparency.get() else 1.0
            properties.set("useTransparency", newValue)
        }
        transparency.addListener { _, _, v ->
            setStageOpacity(primaryStage, v)
            properties.set("transparency", v.toDouble())
        }

        // min max and drag listeners
        registerMaxMinListener(primaryStage)
        registerDragListener(primaryStage)
        // colors
        colorChooser.chosenColor.set(Color.web(properties.getOrSet("falseColor", Color.WHITE.toString())))
        falseColor.bind(colorChooser.chosenColor)
        falseColor.addListener { _, _, v ->
            logger.debug("setting false color $v")
            properties.set("falseColor", v.toString())
        }
        // cursor
        cursorColorChooser.chosenColor.set(Color.web(properties.getOrSet("cursorColor", Color.BLUE.toString())))
        cursorColor.bind(cursorColorChooser.chosenColor)
        cursorColor.addListener { _, _, v ->
            properties.set("cursorColor", v.toString())
        }
        // background
        backgroundColor.bind(blackWhiteChooser.chosenColor)
        backgroundColor.addListener { _, _, v ->
            logger.debug("setting background color $v")
            properties.set("backgroundColor", v.toString())
        }

        // lock window
        lockWindowMenuItem.selectedProperty().set(properties.getOrSet("lockWindow", false))
        lockWindowMenuItem.selectedProperty().addListener { _, _, v ->
            logger.debug("set alwaysOnTop to $v")
            primaryStage.isAlwaysOnTop = v
            properties.set("lockWindow", v)
        }
        // hide while out of focus
        parentPane.hoverProperty().addListener { _, _, v ->
            menuBar.opacity = if (v) 1.0 else 0.0
            mapPane.setCursorHintOpaque(v)
        }
        // cursor visibility
        showCursorText.selectedProperty().set(properties.getOrSet("showCursorText", true))
        showCursorText.selectedProperty().addListener { _, _, v ->
            logger.debug("set alwaysOnTop to $v")
            mapPane.setCursorTextVisible(v)
            properties.set("showCursorText", v)
        }
        enableWaypoint.selectedProperty().set(properties.getOrSet("enableWaypoint", true))
        enableWaypoint.selectedProperty().addListener { _, _, v ->
            mapPane.resetWaypoint()
            properties.set("enableWaypoint", v)
        }

        mapPane.cursor.scaleProperty.bind(cursorScaleSlider.valueProperty().divide(100.0))
        cursorScaleSlider.valueProperty().addListener { _, _, v ->
            mapPane.redraw()
            val scale = (v.toDouble() / 100.0)
            mapPane.setStatusText("Cursor scale: ${scale.toInt()}%")
        }

        when (properties.getOrSet("colorTransformer", "original")) {
            "z" -> zRadio.selectedProperty().set(true)
            else -> originalRadio.selectedProperty().set(true)
        }
        // draw
        mapPane.redraw()

    }

    private val pingRegex = Regex("![Pp][Ii][Nn][Gg] ([a-zA-Z]+) *, *([-.\\d]+) *, *([-.\\d]+)")

    @EventListener
    fun onMessageEvent(messageEvent: MessageEvent) {
        if(!enableWaypoint.selectedProperty().get()) return
        pingRegex.matchEntire(messageEvent.text.trim())?.run {
            onPing(
                messageEvent.type,
                messageEvent.from,
                messageEvent.to,
                groupValues[1],
                groupValues[2],
                groupValues[3]
            )
        }

    }

    private var lastPos: Triple<Double, Double, Long>? = null

    private fun onPing(type: Type, from: String, to: String, zoneName: String, y: String, x: String) {
        if (from == to || from == "You") return // ignore yourself
        if (mapPane.getMapShortName() != zoneName) return // only if current zone
        val ix = -(x.toDoubleOrNull() ?: return) // coordinates
        val iy = -(y.toDoubleOrNull() ?: return)
        logger.debug("ping in $type, from $from at ($zoneName, $iy, $ix)")
        Platform.runLater {
            mapPane.userPing(ix, iy, from)
        }
    }

    @EventListener
    fun onZoneEvent(e: ZoneEvent) {
        logger.debug("${e.playerName} zoning to ${e.zone}")
        zones.firstOrNull { it.name.equals(e.zone, true) }?.also {
            Platform.runLater {
                switchZone(it)
            }
        } ?: kotlin.run {
            logger.error("no mapping for zone '${e.zone}' found in ${zones.map { it.name }}")
        }
    }

    private fun switchZone(it: ZoneMap) {
        mapPane.setMapContent(it)
        populateLayerMenu(it)
        zLayerCheckMenuItem.selectedProperty().set(
            properties.getMap<Boolean>("useZLayerViewDistance").getOrDefault(
                it.shortName, false
            )
        )
    }

    @EventListener
    fun onLocationEvent(e: LocationEvent) {
        logger.debug("${e.playerName} moving to (x=${e.x.roundToInt()}, y=${e.y.roundToInt()}, z=${e.z.roundToInt()})")
        mapPane.moveCursor(e.x, e.y, e.z)

        val speed = lastPos?.let {
            val dt = System.currentTimeMillis() - it.third
            sqrt((it.first - e.x).pow(2) + (it.second - e.y).pow(2)).absoluteValue * 1000 /
                    dt
        } ?: 0.0
        lastPos = Triple(e.x, e.y, System.currentTimeMillis())
        Platform.runLater {
            mapPane.setStatusText("Relative speed: ${"%.2f".format(speed)}m/s")
        }
    }

    @FXML
    fun setZColorTransformer() {
        properties.set("colorTransformer", "z")
        mapPane.setColorTransformer(zColorTransformer)
    }

    @FXML
    fun setOriginalTransformer() {
        properties.set("colorTransformer", "original")
        mapPane.setColorTransformer(OriginalTransformer)
    }

    fun setStageOpacity(primaryStage: Stage, v: Number) {
        if (!transparentWindow.selectedProperty().get()) {
            return
        }
        primaryStage.opacity = v.toDouble()
    }

    private fun populateZoneMenu() {
        zones.forEach { map ->
            val element = MenuItem(map.name.capitalize())
            element.setOnAction {
                switchZone(map)
            }
            zoneMenu.items.add(element)
        }
    }

    private fun populateLayerMenu(map: ZoneMap) {
        poiLayerMenu.items.clear()
        map.layer.sortedWith(LayerComparator).forEach { layer ->
            val name = layer.name.removeSuffix(".txt").lowercase().capitalize()
            val checkMenuItem = CustomCheckMenuItem(name).also {
                it.checkbox.selectedProperty().set(true)
                it.checkbox.selectedProperty().addListener { _, _, v ->
                    layer.show = v
                    mapPane.redraw()
                }
                it.isHideOnClick = false
            }
            poiLayerMenu.items.add(checkMenuItem)
        }
    }

    private fun registerMaxMinListener(primaryStage: Stage) {
        menuBar.setOnMouseClicked {
            if (lockWindowMenuItem.selectedProperty().get()) {
                return@setOnMouseClicked
            }
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                primaryStage.isMaximized = !primaryStage.isMaximized
                mapPane.centerMap()
            }
            if (it.button == MouseButton.SECONDARY && it.clickCount == 2) {
                primaryStage.isIconified = true
            }
            it.consume()
        }
    }

    private fun registerDragListener(primaryStage: Stage) {
        menuBar.setOnMouseDragged {
            if (lockWindowMenuItem.selectedProperty().get()) {
                return@setOnMouseDragged
            }
            primaryStage.x = it.screenX + xOffset;
            primaryStage.y = it.screenY + yOffset;
            it.consume()
        }
        menuBar.setOnMousePressed {
            xOffset = primaryStage.x - it.screenX;
            yOffset = primaryStage.y - it.screenY;
            it.consume()
        }
    }

    @PreDestroy
    fun destroy() {
    }

    @FXML
    fun exit() {
        context.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapController::class.java)
        private val zColorTransformer = ZColorTransformer(30)
    }
}