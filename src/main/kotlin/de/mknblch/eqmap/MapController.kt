package de.mknblch.eqmap

import de.mknblch.eqmap.common.BlackWhiteChooser
import de.mknblch.eqmap.common.ColorChooser
import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.*
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
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
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var colorChooser: ColorChooser

    @FXML
    private lateinit var blackWhiteChooser: BlackWhiteChooser

    @FXML
    private lateinit var showPoi: CheckMenuItem

    @FXML
    private lateinit var centerCheckMenuItem: CheckMenuItem

    @FXML
    private lateinit var zLayerCheckMenuItem: CheckMenuItem

    @FXML
    lateinit var lockWindowMenuItem: CheckMenuItem

    @FXML
    private lateinit var transparentWindow: CheckMenuItem

    @Qualifier("useZLayerViewDistance")
    @Autowired
    private lateinit var  useZLayerViewDistance : SimpleBooleanProperty

    @Qualifier("centerPlayerCursor")
    @Autowired
    private lateinit var  centerPlayerCursor : SimpleBooleanProperty

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
            }
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
        useZLayerViewDistance.addListener { _,_,_ ->
            mapPane.showAllNodes()
        }
        val primaryStage: Stage = context.getBean("primaryStage") as Stage
        transparentWindow.selectedProperty().addListener { _, _, newValue ->
            primaryStage.opacity = if(newValue) 0.7 else 1.0
        }
        registerMaximizeListener(primaryStage)
        registerDragListener(primaryStage)
        colorChooser.chosenColorProperty().addListener { _, _, v ->
            mapPane.deriveColor(v)
        }
        blackWhiteChooser.chosenColorProperty().addListener { _, _, v ->
            logger.debug("setting background to $v")
            mapPane.background = Background.fill(v)
        }
        lockWindowMenuItem.selectedProperty().addListener { _, _, v ->
            primaryStage.isAlwaysOnTop = v
        }
        parentPane.hoverProperty().addListener { _, _, v ->
            menuBar.opacity = if (v) 1.0 else 0.0
        }
        resetMenuItem.setOnAction {
            directoryWatcherService.reset()
        }
        mapPane.showAllNodes()
    }

    private fun populateZoneMenu() {
        zones.forEach { map ->
            val element = MenuItem(map.name)
            element.setOnAction {
                mapPane.setMapContent(map)
            }
            zoneMenu.items.add(element)
        }
    }

    private fun registerMaximizeListener(primaryStage: Stage) {
        menuBar.setOnMouseClicked {
            if (lockWindowMenuItem.selectedProperty().get()) {
                return@setOnMouseClicked
            }
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                primaryStage.isMaximized = !primaryStage.isMaximized
                mapPane.centerMap()
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