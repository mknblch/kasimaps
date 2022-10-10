package de.mknblch.eqmap

import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.config.DirectoryWatcherService
import de.mknblch.eqmap.config.SpringFXMLLoader
import de.mknblch.eqmap.zone.ZoneMap
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import java.io.File
import javax.annotation.PreDestroy


@SpringBootApplication
class Kasimaps : CommandLineRunner {

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Autowired
    private lateinit var zones: List<ZoneMap>

    @Autowired
    private lateinit var loader: SpringFXMLLoader

    @Autowired
    private lateinit var properties: PersistentProperties

    @Autowired
    private lateinit var directoryWatcherService: DirectoryWatcherService

    private lateinit var stage: Stage
    private lateinit var scene: Scene

    fun start() {
        stage = Stage(StageStyle.TRANSPARENT).also { s ->
            properties.get<Double>("x")?.also {
                s.x = it
            }
            properties.get<Double>("y")?.also {
                s.y = it
            }
        }


        context.beanFactory.registerSingleton("primaryStage", stage)
        val (root, mapController) = loader.load(MapController::class.java)
        scene = Scene(
            root,
            properties.getOrSet("width", 800.0),
            properties.getOrSet("height", 600.0),
            Color.TRANSPARENT
        )
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")!!.toExternalForm())
        scene.fill = Color.TRANSPARENT
        stage.scene = scene
        stage.show()

        properties.getOrEval("eqDirectory") {
            chooseEqDirectory()
        }?.also {
            directoryWatcherService.start(File(it))
        }

        // reset
        mapController.resetMenuItem.setOnAction {
            logger.info("reset event parser")
            chooseEqDirectory()?.also {
                properties.set("eqDirectory", it)
                directoryWatcherService.start(File(it))
            }
        }

        // TODO replace with something that works
        ResizeHelper.addResizeListener(mapController.lockWindowMenuItem.selectedProperty(), stage)

        // RODO refactor
        mapController.mapPane.setMapContent(zones[0])
        stage.isAlwaysOnTop = properties.getOrSet("lockWindow", false)
        mapController.setStageOpacity(stage, properties.getOrSet("transparency", 0.9))
        mapController.mapPane.setCursorTextVisible(properties.getOrSet("showCursorText", true))
        mapController.mapPane.setBackgroundColor(Color.web(properties.getOrSet("backgroundColor", "#BABABA")))
        mapController.mapPane.deriveColor(Color.web(properties.getOrSet("falseColor", "goldenrod")))
    }

    private fun chooseEqDirectory(): String? {
        val fileChooser = DirectoryChooser().also {
            it.title = "Set EQ root directory"
        }
        return fileChooser.showDialog(stage)?.absolutePath ?: kotlin.run {
            logger.warn("EQ-Directory not set - Interactive mode will not be available!")
            null
        }
    }

    @PreDestroy
    fun stop() {
        properties.set("width", scene.width)
        properties.set("height", scene.height)
        properties.set("x", stage.x)
        properties.set("y", stage.y)

        properties.write()
        Platform.exit()
    }

    override fun run(vararg args: String?) {
        Platform.startup(this::start);
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Kasimaps::class.java)
    }
}

fun main(args: Array<out String>) {
    SpringApplication.run(Kasimaps::class.java, *args)
}