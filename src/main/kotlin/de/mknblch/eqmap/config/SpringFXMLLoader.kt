package de.mknblch.eqmap.config

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.util.Callback
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class FxmlResource(val path: String)

@Component
class SpringFXMLLoader @Autowired constructor(
    private val resourceBundle: ResourceBundle,
    private val beanFactory: AutowireCapableBeanFactory,
) {

    @Throws(IOException::class)
    fun <T> load(fxmlPath: String, clazz: Class<T>): Pair<Parent, T> {
        logger.trace("loading ${clazz.simpleName}")
        val loader = FXMLLoader()
        val controller: T = beanFactory.getBean(clazz) ?: throw IllegalArgumentException("Controller not found $clazz")
        with(loader) {
            setController(controller)
            controllerFactory = Callback { beanFactory.getBean(it) }
            resources = resourceBundle
            location = javaClass.classLoader.getResource(fxmlPath)
        }
        return Pair(loader.load(), controller)
    }

    @Throws(IOException::class)
    fun <T> load(clazz: Class<T>): Pair<Parent, T> {
        val fxmlResource: FxmlResource = clazz.getDeclaredAnnotation(FxmlResource::class.java)
            ?: throw IllegalArgumentException("Controller class must be annotated with @FxmlResource(..)")
        return load(fxmlResource.path, clazz)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpringFXMLLoader::class.java)
    }
}