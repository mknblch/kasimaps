package de.mknblch.eqmap

import de.mknblch.eqmap.common.ColorChooser
import de.mknblch.eqmap.config.FxmlResource
import de.mknblch.eqmap.config.ZoneMap
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.stage.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import java.net.URL
import java.util.*


@Controller
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


    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        populateZoneMenu()
        menuBar.opacity = 1.0
        // register properties
        mapPane.centerPlayerCursor.bind(centerCheckMenuItem.selectedProperty())
        mapPane.useZLayerViewDistance.bind(zLayerCheckMenuItem.selectedProperty())
        mapPane.useZLayerViewDistance.addListener { _,_,_ ->
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


    @FXML
    fun exit() {
        context.close()
        Platform.exit()
    }
}