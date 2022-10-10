package de.mknblch.eqmap

import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.PersistentProperties
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
import javafx.beans.property.ObjectProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
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

    @Qualifier("transparency")
    @Autowired
    private lateinit var transparency: SimpleDoubleProperty

    @Qualifier("falseColor")
    @Autowired
    private lateinit var falseColor: ObjectProperty<Color>

    @Qualifier("backgroundColor")
    @Autowired
    private lateinit var backgroundColor: ObjectProperty<Color>

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
        // zlayer
        zLayerCheckMenuItem.selectedProperty().set(properties.getOrSet("useZLayerViewDistance", false))
        zLayerCheckMenuItem.selectedProperty().addListener { _, _, v ->
            properties.set("useZLayerViewDistance", v)
        }

        showPoi.selectedProperty().set(properties.getOrSet("showPoi", true))
        showPoi.selectedProperty().addListener { _, _, v ->
            properties.set("showPoi", v)
        }

        // register properties
        centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
        useZLayerViewDistance.bind(zLayerCheckMenuItem.selectedProperty())
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
            properties.set("opacity", v.toDouble())
        }
        // min max and drag listeners
        registerMaxMinListener(primaryStage)
        registerDragListener(primaryStage)
        // colors
        falseColor.bind(colorChooser.chosenColorProperty())
        falseColor.addListener { _, _, v ->
            logger.debug("setting false color $v")
            properties.set("falseColor", v.toString())
        }
        // background
        backgroundColor.bind(blackWhiteChooser.chosenColorProperty())
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
            mapPane.setCursorOpaque(v)
        }
        // cursor visibility
        showCursorText.selectedProperty().set(properties.getOrSet("showCursorText", true))
        showCursorText.selectedProperty().addListener { _, _, v ->
            logger.debug("set alwaysOnTop to $v")
            mapPane.setCursorTextVisible(v)
            properties.set("showCursorText", v)
        }
        // draw
        mapPane.redraw()


    }

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
                mapPane.setMapContent(map)
                populateLayerMenu(map)
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