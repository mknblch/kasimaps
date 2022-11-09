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
        logger.debug("loading ${clazz.simpleName}")
        val loader = FXMLLoader(javaClass.classLoader.getResource(fxmlPath))
        val controller: T = beanFactory.getBean(clazz) ?: throw IllegalArgumentException("Controller not found $clazz")
        with(loader) {
            controllerFactory = Callback { controller }
            resources = resourceBundle
//            location = javaClass.classLoader.getResource(fxmlPath)
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
        logger.debug("readImportProcessingInstruction $target")
    }

    override fun readLanguageProcessingInstruction(language: String?) {
        logger.debug("readLanguageProcessingInstruction $language")
    }

    override fun readComment(comment: String?) {
        logger.debug("readComment $comment")
    }

    override fun beginInstanceDeclarationElement(type: Class<*>?) {
        logger.debug("beginInstanceDeclarationElement ${type?.simpleName}")
    }

    override fun beginUnknownTypeElement(name: String?) {
        logger.debug("beginUnknownTypeElement $name")
    }

    override fun beginIncludeElement() {
        logger.debug("beginIncludeElement")
    }

    override fun beginReferenceElement() {
        logger.debug("beginReferenceElement")
    }

    override fun beginCopyElement() {
        logger.debug("beginCopyElement")
    }

    override fun beginRootElement() {
        logger.debug("beginRootElement")
    }

    override fun beginPropertyElement(name: String?, sourceType: Class<*>?) {
        logger.debug("beginPropertyElement $name ${sourceType?.simpleName}")
    }

    override fun beginUnknownStaticPropertyElement(name: String?) {
        logger.debug("beginUnknownStaticPropertyElement $name")
    }

    override fun beginScriptElement() {
        logger.debug("beginScriptElement")
    }

    override fun beginDefineElement() {
        logger.debug("beginDefineElement")
    }

    override fun readInternalAttribute(name: String?, value: String?) {
        logger.debug("readInternalAttribute $name $value")
    }

    override fun readPropertyAttribute(name: String?, sourceType: Class<*>?, value: String?) {
        logger.debug("readPropertyAttribute $name")
    }

    override fun readUnknownStaticPropertyAttribute(name: String?, value: String?) {
        logger.debug("readUnknownStaticPropertyAttribute $name")
    }

    override fun readEventHandlerAttribute(name: String?, value: String?) {
        logger.debug("readEventHandlerAttribute $name")
    }

    override fun endElement(value: Any) {
        val objectId = System.identityHashCode(value).toString(16)
        logger.debug("autowiring ${value.javaClass}#$objectId")
        beanFactory.autowireBean(value)
        beanFactory.initializeBean(value, objectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutowiringLoadListener::class.java)
    }
}