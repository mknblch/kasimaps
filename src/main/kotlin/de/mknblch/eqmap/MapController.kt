package de.mknblch.eqmap

import de.mknblch.eqmap.common.ColorChooser
import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.FxmlResource
import de.mknblch.eqmap.config.ZoneMap
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.Background
import javafx.scene.paint.Color
import javafx.stage.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import java.net.URL
import java.util.*
import javax.annotation.PreDestroy


@Component
@FxmlResource("fxml/Map.fxml")
class MapController : Initializable {

    @Autowired
    private lateinit var zones: List<ZoneMap>

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @FXML
    private lateinit var mapPane: MapPane

    @FXML
    private lateinit var zoneMenu: Menu

    @FXML
    private lateinit var menuBar: MenuBar

    @FXML
    private lateinit var colorChooser: ColorChooser

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

    private var xOffset: Double = 0.0
    private var yOffset: Double = 0.0

    private val zoneRegex = Regex("You have entered (.+)\\.")
    private val locRegex = Regex("Your Location is ([^,]+), ([^,]+), ([^,]+)")

    @EventListener
    fun onEqEvent(event: EqEvent) {
        if (event.origin == Origin.EQLOG) {

            zoneRegex.matchEntire(event.text)?.run {
                val zoneName = groupValues[1]
                println("zone to $zoneName")
                zones.firstOrNull { it.name.equals(zoneName, true) }?.also {
                    Platform.runLater {
                        mapPane.setMapContent(it)
                    }
                }
            }

            locRegex.matchEntire(event.text)?.run {
                val y = -groupValues[1].toDouble()
                val x = -groupValues[2].toDouble()
                val z = groupValues[3].toDouble()
                mapPane.moveCursor(x, y, z)
            }
        }

    }

    @FXML
    fun setZColorTransformer() {
        mapPane.setColorTransformer(ZColorTransformer(30))
    }

    @FXML
    fun setOriginalTransformer() {

        mapPane.setColorTransformer(OriginalTransformer)
    }


    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        populateZoneMenu()
        menuBar.opacity = 1.0
        // register properties
        centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
        useZLayerViewDistance.bind(zLayerCheckMenuItem.selectedProperty())
        useZLayerViewDistance.addListener { _,_,_ ->
            mapPane.showAllNodes()
        }
        val primaryStage: Stage = context.getBean("primaryStage") as Stage
        transparentWindow.selectedProperty().addListener { _, _, newValue ->
            primaryStage.opacity = if(newValue) 0.8 else 1.0
        }
        registerMaximizeListener(primaryStage)
        registerDragListener(primaryStage)
        colorChooser.chosenColorProperty().addListener { _,_,v ->
            mapPane.deriveColor(v)
        }
        lockWindowMenuItem.selectedProperty().addListener { _, _, newValue ->
            primaryStage.isAlwaysOnTop = newValue
        }

        mapPane.background = Background.fill(Color.TRANSPARENT)
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
            if (it.clickCount == 2) {
                primaryStage.isMaximized = !primaryStage.isMaximized
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
}