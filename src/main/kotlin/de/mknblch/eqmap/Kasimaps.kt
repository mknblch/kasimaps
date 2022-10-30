package de.mknblch.eqmap

import de.mknblch.eqmap.common.OriginalTransformer
import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.common.ZColorTransformer
import de.mknblch.eqmap.config.DirectoryWatcherService
import de.mknblch.eqmap.config.SpringFXMLLoader
import de.mknblch.eqmap.zone.ZoneMap
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.Resource
import java.io.File
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@SpringBootApplication
class Kasimaps : CommandLineRunner {

    @Value("classpath:dragon_ico.png")
    private lateinit var icon: Resource

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

    @PostConstruct
    fun init() {
    }

    fun start() {
        stage = Stage(StageStyle.TRANSPARENT).also { s ->
            properties.get<Double>("x")?.also {
                s.x = it
            }
            properties.get<Double>("y")?.also {
                s.y = it
            }
            s.icons.add(Image(icon.inputStream))
        }

        context.beanFactory.registerSingleton("primaryStage", stage)
        val (root, mapController) = loader.load(MapController::class.java)

        scene = Scene(
            root,
            properties.getOrSet("width", 800.0),
            properties.getOrSet("height", 600.0),
            Color.TRANSPARENT
        )
        scene.fill = null //Color.TRANSPARENT
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")!!.toExternalForm())
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

        // TODO refactor
        Platform.runLater {

            mapController.mapPane.setMapContent(zones[0])
            stage.isAlwaysOnTop = properties.getOrSet("lockWindow", false)
            mapController.mapPane.setCursorTextVisible(properties.getOrSet("showCursorText", true))
            val colorTransformer = when (properties.getOrSet("colorTransformer", "original")) {
                "z" -> ZColorTransformer(30)
                else -> OriginalTransformer
            }
            mapController.mapPane.setColorTransformer(colorTransformer)
        }
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