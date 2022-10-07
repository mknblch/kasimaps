package de.mknblch.eqmap

import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.DirectoryWatcherService
import de.mknblch.eqmap.config.FxmlResource
import de.mknblch.eqmap.config.LocationEvent
import de.mknblch.eqmap.config.ZoneEvent
import de.mknblch.eqmap.fx.BlackWhiteChooser
import de.mknblch.eqmap.fx.ColorChooser
import de.mknblch.eqmap.fx.CustomCheckMenuItem
import de.mknblch.eqmap.fx.MapPane
import de.mknblch.eqmap.zone.LayerComparator
import de.mknblch.eqmap.zone.ZoneMap
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
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
import java.net.URL
import java.util.*
import javax.annotation.PreDestroy
import kotlin.math.roundToInt


@Component
@FxmlResource("fxml/Map.fxml")
class MapController : Initializable {

    @Autowired
    private lateinit var zones: List<ZoneMap>

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Autowired
    private lateinit var directoryWatcherService: DirectoryWatcherService

    @FXML
    private lateinit var parentPane: StackPane

    @FXML
    private lateinit var mapPane: MapPane

    @FXML
    private lateinit var resetMenuItem: MenuItem

    @FXML
    private lateinit var zoneMenu: Menu

    @FXML
    private lateinit var poiLayerMenu: Menu

    @FXML
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var colorChooser: ColorChooser

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

    @Qualifier("useZLayerViewDistance")
    @Autowired
    private lateinit var useZLayerViewDistance: SimpleBooleanProperty

    @Qualifier("centerPlayerCursor")
    @Autowired
    private lateinit var centerPlayerCursor: SimpleBooleanProperty

    @Qualifier("showPoiProperty")
    @Autowired
    private lateinit var showPoiProperty: SimpleBooleanProperty

    private var xOffset: Double = 0.0
    private var yOffset: Double = 0.0

    @EventListener
    fun onZoneEvent(e: ZoneEvent) {
        logger.debug("${e.playerName} zoning to ${e.zone}")
        zones.firstOrNull { it.name.equals(e.zone, true) }?.also {
            Platform.runLater {
                mapPane.setMapContent(it)
                populateLayerMenu(it)
            }
        } ?: kotlin.run {
            logger.error("no mapping for zone '${e.zone}' found in ${zones.map { it.name }}")
        }
    }

    @EventListener
    fun onLocationEvent(e: LocationEvent) {
        logger.debug("${e.playerName} moving to (x=${e.x.roundToInt()}, y=${e.y.roundToInt()}, z=${e.z.roundToInt()})")
        mapPane.moveCursor(e.x, e.y, e.z)
    }

    @FXML
    fun setZColorTransformer() {
        mapPane.setColorTransformer(Companion.zColorTransformer)
    }

    @FXML
    fun setOriginalTransformer() {
        mapPane.setColorTransformer(OriginalTransformer)
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        populateZoneMenu()
        menuBar.opacity = 1.0
        mapPane.background = Background.EMPTY
        // register properties
        centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
        useZLayerViewDistance.bind(zLayerCheckMenuItem.selectedProperty())
        showPoiProperty.bind(showPoi.selectedProperty())
        useZLayerViewDistance.addListener { _, _, _ ->
            mapPane.redraw()
        }
        val primaryStage: Stage = context.getBean("primaryStage") as Stage
        transparentWindow.selectedProperty().addListener { _, _, newValue ->
            primaryStage.opacity = if (newValue) 0.7 else 1.0
        }
        registerMaxMinListener(primaryStage)
        registerDragListener(primaryStage)
        colorChooser.chosenColorProperty().addListener { _, _, v ->
            mapPane.deriveColor(v)
        }
        blackWhiteChooser.chosenColorProperty().addListener { _, _, v ->
            logger.debug("setting background to $v")
            mapPane.setBackgroundColor(v)
        }
        lockWindowMenuItem.selectedProperty().addListener { _, _, v ->
            primaryStage.isAlwaysOnTop = v
        }
        parentPane.hoverProperty().addListener { _, _, v ->
            menuBar.opacity = if (v) 1.0 else 0.0
            mapPane.setCursorVisible(v)
        }
        resetMenuItem.setOnAction {
            directoryWatcherService.reset()
        }
        showCursorText.selectedProperty().addListener { _, _, v ->
            mapPane.shotCursorText(v)
        }
//        parentPane.border = Border(BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths(5.0)))
        mapPane.redraw()
    }

    private fun populateZoneMenu() {
        zones.forEach { map ->
            val element = MenuItem(map.name)
            element.setOnAction {
                mapPane.setMapContent(map)
                populateLayerMenu(map)
            }
            zoneMenu.items.add(element)
        }
    }

    private fun populateLayerMenu(map: ZoneMap) {
        poiLayerMenu.items.clear()
        map.layer.sortedWith(LayerComparator).forEach { layer ->
            val checkMenuItem = CustomCheckMenuItem(layer.name.removeSuffix(".txt")).also {
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
        Platform.exit()
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