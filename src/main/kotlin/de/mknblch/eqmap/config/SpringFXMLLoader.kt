package de.mknblch.eqmap.config

import javafx.fxml.FXMLLoader
import javafx.fxml.LoadListener
import javafx.scene.Parent
import javafx.util.Callback
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.util.*

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class FxmlResource(val path: String)


@Scope("singleton")
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
            controllerFactory = Callback {
                beanFactory.getBean(it)
            }
            resources = resourceBundle
            location = javaClass.classLoader.getResource(fxmlPath)
            loadListener = AutowiringLoadListener(beanFactory)
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

class AutowiringLoadListener(private val beanFactory: AutowireCapableBeanFactory) : LoadListener {

    override fun readImportProcessingInstruction(target: String?) {
    }

    override fun readLanguageProcessingInstruction(language: String?) {
    }

    override fun readComment(comment: String?) {
    }

    override fun beginInstanceDeclarationElement(type: Class<*>?) {
    }

    override fun beginUnknownTypeElement(name: String?) {
    }

    override fun beginIncludeElement() {
    }

    override fun beginReferenceElement() {
    }

    override fun beginCopyElement() {
    }

    override fun beginRootElement() {
    }

    override fun beginPropertyElement(name: String?, sourceType: Class<*>?) {
    }

    override fun beginUnknownStaticPropertyElement(name: String?) {
    }

    override fun beginScriptElement() {
    }

    override fun beginDefineElement() {
    }

    override fun readInternalAttribute(name: String?, value: String?) {
    }

    override fun readPropertyAttribute(name: String?, sourceType: Class<*>?, value: String?) {
    }

    override fun readUnknownStaticPropertyAttribute(name: String?, value: String?) {
    }

    override fun readEventHandlerAttribute(name: String?, value: String?) {
    }

    override fun endElement(value: Any) {
        val objectId = System.identityHashCode(value).toString(16)
        logger.trace("autowiring ${value.javaClass}#$objectId")
        beanFactory.autowireBean(value)
        beanFactory.initializeBean(value, objectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutowiringLoadListener::class.java)
    }
}