package de.mknblch.eqmap

import de.mknblch.eqmap.config.SpringFXMLLoader
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext
import javax.annotation.PreDestroy


@SpringBootApplication
class Kasimaps : CommandLineRunner {

    @Autowired
    private lateinit var context: ConfigurableApplicationContext

    @Autowired
    private lateinit var loader: SpringFXMLLoader

    fun start() {
        val stage = Stage(StageStyle.TRANSPARENT)
        context.beanFactory.registerSingleton("primaryStage", stage)
        val (root, mapController) = loader.load(MapController::class.java)
        val scene = Scene(root, 800.0, 600.0, Color.TRANSPARENT)
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")!!.toExternalForm())
        scene.fill = Color.TRANSPARENT
        stage.scene = scene
        stage.initStyle(StageStyle.TRANSPARENT)
        stage.show()

        ResizeHelper.addResizeListener(mapController.lockWindowMenuItem.selectedProperty(), stage)
    }

    @PreDestroy
    fun stop() {
        Platform.exit()
    }

    override fun run(vararg args: String?) {
        Platform.startup(this::start);
    }

}

fun main(args: Array<out String>) {
    SpringApplication.run(Kasimaps::class.java, *args)
}