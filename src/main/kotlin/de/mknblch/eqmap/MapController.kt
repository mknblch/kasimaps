package de.mknblch.eqmap

import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.*
import de.mknblch.eqmap.fx.*
import de.mknblch.eqmap.zone.LayerComparator
import de.mknblch.eqmap.zone.MapPOI
import de.mknblch.eqmap.zone.ZoneMap
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Point2D
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.URL
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class StatusEvent(val statusText: String)

@Component
@FxmlResource("fxml/Map.fxml")
class MapController : Initializable {

    @Autowired
    private lateinit var loader: SpringFXMLLoader

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Autowired
    private lateinit var zones: List<ZoneMap>

    @Autowired
    private lateinit var properties: PersistentProperties

    @Autowired
    private lateinit var networkSyncService: NetworkSyncService

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
    private lateinit var findMenu: Menu

    @FXML
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var colorChooser: ColorChooser

    @FXML
    private lateinit var cursorColorChooser: ColorChooser

    @FXML
    private lateinit var blackWhiteChooser: BlackWhiteChooser

    @FXML
    private lateinit var syncMenuItem: CustomCheckMenuItem

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
    private lateinit var enableWaypoint: CustomCheckMenuItem

    @FXML
    private lateinit var zRadio: RadioMenuItem

    @FXML
    private lateinit var originalRadio: RadioMenuItem

    @FXML
    private lateinit var cursorScaleSlider: Slider

    @FXML
    private lateinit var alphaSlider: Slider

    private var xOffset: Double = 0.0
    private var yOffset: Double = 0.0
    private var lastPos: Triple<Double, Double, Long>? = null

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        populateZoneMenu()
        properties.bind("alpha", 0.8, mapPane.alpha)
        alphaSlider.valueProperty().set(mapPane.alpha.value * 255.0)
        mapPane.alpha.bind(alphaSlider.valueProperty().divide(255.0))
        zLayerCheckMenuItem.selectedProperty().addListener { _, _, v: Boolean ->
            mapPane.getMapShortName()?.also {
                properties.getMap<Boolean>("useZLayerViewDistance")[it] = v
                mapPane.useZLayerViewDistance[it] = v
                mapPane.redraw()
            }
        }
        properties.bind("showPoi", true, showPoi.selectedProperty())
        mapPane.showPoiProperty.bind(showPoi.selectedProperty())
        properties.bind("centerPlayerCursor", true, centerCheckMenuItem.selectedProperty())
        // register properties
        mapPane.centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
        mapPane.showPoiProperty.bind(showPoi.selectedProperty())
        //
        val primaryStage: Stage = context.getBean("primaryStage") as Stage
        // min max and drag listeners
        registerMenuBarClickListener(primaryStage)
        registerMenuBarDragListener(primaryStage)
        // colors
        properties.bind("falseColor", Color.RED, colorChooser.chosenColor)
        mapPane.falseColor.bind(colorChooser.chosenColor)
        // cursor
        properties.bind("cursorColor", Color.WHITE, cursorColorChooser.chosenColor)
        mapPane.cursorColor.bind(cursorColorChooser.chosenColor)
        // background
        properties.bind("backgroundColor", Color.BLACK, blackWhiteChooser.chosenColor)
        mapPane.backgroundColor.bind(blackWhiteChooser.chosenColor)

        // lock window
        properties.bind("lockWindow", false, lockWindowMenuItem.selectedProperty());
        lockWindowMenuItem.selectedProperty().addListener { _, _, v ->
            primaryStage.isAlwaysOnTop = v
        }
        // hide while out of focus toggle
        parentPane.hoverProperty().addListener { _, _, v ->
            menuBar.opacity = if (v) 1.0 else 0.0
            mapPane.setCursorHintOpaque(v)
        }
        // cursor visibility toggle
        properties.bind("showCursorText", true, showCursorText.selectedProperty())
        showCursorText.selectedProperty().addListener { _, _, v ->
            mapPane.setCursorTextVisible(v)
        }
        // chat waypoint toggle
        properties.bind("enableWaypoint", true, enableWaypoint.selectedProperty())
        enableWaypoint.selectedProperty().addListener { _, _, v ->
            mapPane.resetWaypoint()
        }
        properties.bind("cursorScale", 1.0, cursorScaleSlider.valueProperty())
        mapPane.cursor.scaleProperty.bind(cursorScaleSlider.valueProperty().divide(100.0))
        cursorScaleSlider.valueProperty().addListener { _, _, v ->
            mapPane.setStatusText("Cursor scale: ${v.toInt()}%")
        }

        syncMenuItem.selectedProperty().addListener { _, _, v ->
            if (v) {
                val (_, networkDialogController) = loader.load(NetworkDialogController::class.java)
                networkDialogController.show(primaryStage)?.also(networkSyncService::connect) ?: kotlin.run {
                    syncMenuItem.selectedProperty().set(false)
                }
            } else {
                networkSyncService.disconnect()
            }
        }
        when (properties.getOrSet("colorTransformer", "z")) {
            "z" -> zRadio.selectedProperty().set(true)
            else -> originalRadio.selectedProperty().set(true)
        }
        // draw
        mapPane.redraw()
    }

    @EventListener
    fun onStatusEvent(statusEvent: StatusEvent) {
        Platform.runLater {
            mapPane.setStatusText(statusEvent.statusText)
        }
    }

    @EventListener
    fun onIrcLocationEvent(ircLocationEvent: IRCLocationEvent) {
        // only track events of current zone
        if (mapPane.getMapShortName() != ircLocationEvent.zone) return
        Platform.runLater {
            mapPane.setIrcPlayerMarker(
                ircLocationEvent.player,
                ircLocationEvent.x,
                ircLocationEvent.y
            )
        }
    }

    @EventListener
    fun onIrcZoneEvent(ircZoneEvent: IRCZoneEvent) {
        // only track events of palyers leaving the current zone
        if (mapPane.getMapShortName() == ircZoneEvent.zone) return
        Platform.runLater {
            mapPane.removeIrcPlayerMarker(ircZoneEvent.player)
        }
    }


    @EventListener
    fun onMessageEvent(messageEvent: GameMessageEvent) {
        if (!enableWaypoint.selectedProperty().get()) return
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


    @EventListener
    fun onWhoEvent(e: WhoEvent) {
        zones.firstOrNull { it.shortName.equals(e.zone, true) }?.also {
            Platform.runLater {
                switchZone(it)
            }
        } ?: kotlin.run {
            logger.error("no mapping for zone '${e.zone}' found in ${zones.map { it.name }}")
        }
    }

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

    private fun switchZone(it: ZoneMap) {
        if (mapPane.getMapShortName() == it.shortName) {
            return
        }
        mapPane.setMapContent(it)
        populateLayerMenu(it)
        populateFindMenu(it)
        setZLayerMenuItem(it)

        networkSyncService.setZone(it.shortName)
    }

    private fun setZLayerMenuItem(it: ZoneMap) {
        zLayerCheckMenuItem.selectedProperty().set(
            properties.getMap<Boolean>("useZLayerViewDistance").getOrDefault(
                it.shortName, false
            )
        )
    }

    @EventListener
    fun onLocationEvent(e: LocationEvent) {
        logger.debug("${e.playerName} moving to (x=${e.x.roundToInt()}, y=${e.y.roundToInt()}, z=${e.z.roundToInt()})")

        val speed = lastPos?.let {
            val dt = System.currentTimeMillis() - it.third
            sqrt((it.first - e.x).pow(2) + (it.second - e.y).pow(2)).absoluteValue * 1000 /
                    dt
        } ?: 0.0
        lastPos = Triple(e.x, e.y, System.currentTimeMillis())
        Platform.runLater {
            mapPane.moveCursor(e.x, e.y, e.z)
            mapPane.setStatusText("Speed: ${"%.2f".format(speed)}p/s")
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

    private fun populateFindMenu(map: ZoneMap) {
        findMenu.items.clear()
        map.layer.flatMap { it.nodes }.filterIsInstance<MapPOI>().flatMap { poi ->
            poi.names.map { Pair(it, poi) }
        }.groupBy { it.first }.toSortedMap().forEach { entry ->
            val poiList = entry.value
            val name = entry.key + if(poiList.size > 1) "(${poiList.size})" else ""
            val menuItem = MenuItem(name)
            menuItem.addEventHandler(ActionEvent.ACTION) { _ ->
                mapPane.resetFindMarker()
                poiList.forEach { pair ->
                    mapPane.setFindMarker(
                        x = pair.second.x,
                        y = pair.second.y,
                        name = entry.key
                    )
                }
                poiList.firstOrNull()?.also {
                    mapPane.centerPoint(
                        mapPane.mapToLayout(Point2D(
                            it.second.x,
                            it.second.y
                        ))
                    )
                }
            }
            findMenu.items.add(menuItem)
        }
    }

    private fun registerMenuBarClickListener(primaryStage: Stage) {
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

    private fun registerMenuBarDragListener(primaryStage: Stage) {
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

    @FXML
    fun exit() {
        context.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MapController::class.java)
        private val zColorTransformer = ZColorTransformer(30)
        private val pingRegex = Regex("![Pp][Ii][Nn][Gg] ([a-zA-Z]+) *, *([-.\\d]+) *, *([-.\\d]+)")
    }
}