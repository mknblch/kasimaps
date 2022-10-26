package de.mknblch.eqmap.config

import com.sun.javafx.geometry.BoundsUtils
import de.mknblch.eqmap.MapController
import de.mknblch.eqmap.common.PersistentProperties
import de.mknblch.eqmap.common.ZColorTransformer
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PropertyConfig {

    private val properties = PersistentProperties.load("config.json").also {
        logger.debug("loaded ${it.data}")
    }

    private val cursorColor = SimpleObjectProperty(Color.web(properties.getOrSet("cursorColor", "#3322FF")))
    private val falseColor = SimpleObjectProperty(Color.web(properties.getOrSet("falseColor", "#000000")))
    private val backgroundColor = SimpleObjectProperty(Color.web(properties.getOrSet("backgroundColor", "#BFBFBF"))).also {
            logger.debug("background color loaded: $it")
    }

    private val alpha = SimpleDoubleProperty(properties.getOrSet("alpha", 1.0))
    private val zViewDistance = SimpleDoubleProperty(35.0)
    private val strokeWidthProperty = SimpleDoubleProperty(1.0)
    private val useZLayerViewDistance = SimpleMapProperty<String, Boolean>(FXCollections.observableHashMap())
    private val centerPlayerCursor = SimpleBooleanProperty(true)
    private val showPoiProperty = SimpleBooleanProperty(true)
    private val showCursorHint = SimpleBooleanProperty(true)
    private val pingOnMove = SimpleBooleanProperty(true)


    @Bean
    fun properties(): PersistentProperties = properties

    @Bean(name = ["zViewDistance"])
    fun zViewDistance(): SimpleDoubleProperty = zViewDistance

    @Bean(name = ["strokeWidthProperty"])
    fun strokeWidthProperty(): SimpleDoubleProperty = strokeWidthProperty

    @Bean(name = ["useZLayerViewDistance"])
    fun useZLayerViewDistance() = useZLayerViewDistance

    @Bean(name = ["centerPlayerCursor"])
    fun centerPlayerCursor() = centerPlayerCursor

    @Bean(name = ["showPoiProperty"])
    fun showPoiProperty() = showPoiProperty

    @Bean(name = ["showCursorHint"])
    fun showCursorHint() = showCursorHint

    @Bean(name = ["alpha"])
    fun alpha() = alpha

    @Bean(name = ["falseColor"])
    fun falseColor() = falseColor

    @Bean(name = ["backgroundColor"])
    fun backgroundColor() = backgroundColor

    @Bean(name = ["cursorColor"])
    fun cursorColor() = cursorColor

    @Bean(name = ["pingOnMove"])
    fun pingOnMove() = pingOnMove


    companion object {
        private val logger = LoggerFactory.getLogger(PropertyConfig::class.java)
    }
}