package de.mknblch.eqmap

import de.mknblch.eqmap.config.SpringFXMLLoader
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext


@SpringBootApplication
class FXApplication : Application() {

    private lateinit var context: ConfigurableApplicationContext

    override fun init() {

        context = SpringApplicationBuilder()
            .sources(FXApplication::class.java)
            .run(*parameters.raw.toTypedArray())
    }

    override fun start(stage: Stage) {
        val loader = context.getBean(SpringFXMLLoader::class.java)
        context.beanFactory.registerSingleton("primaryStage", stage)
        val (root, mapController) = loader.load(MapController::class.java)
        val scene = Scene(root, 800.0, 600.0, Color.TRANSPARENT)
        scene.stylesheets.add(javaClass.classLoader.getResource("style.css")!!.toExternalForm())
        scene.fill = Color.TRANSPARENT
        stage.scene = scene
        stage.initStyle(StageStyle.TRANSPARENT)
        stage.show()

        ResizeHelper.addResizeListener(mapController.lockWindowMenuItem.selectedProperty(), stage)

        mapController.lockWindowMenuItem.selectedProperty().addListener { _, _, newValue ->
            stage.isAlwaysOnTop = newValue
        }
    }

    override fun stop() {
        context.close()
        Platform.exit()
    }

}

fun main(args: Array<out String>) {
    Application.launch(FXApplication::class.java, *args)
}