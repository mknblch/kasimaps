package de.mknblch.eqmap.config

import com.sun.javafx.geometry.BoundsUtils
import de.mknblch.eqmap.common.PersistentProperties
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PropertyConfig {

    private val falseColor = SimpleObjectProperty(Color.BLACK)
    private val backgroundColor = SimpleObjectProperty(Color.GRAY)

    private val windowPosition = SimpleObjectProperty(Pair(0.0, 0.0))
    private val windowSize = SimpleObjectProperty(Pair(0.0, 0.0))
    private val transparency = SimpleDoubleProperty(0.75)

    private val zViewDistance = SimpleDoubleProperty(35.0)
    private val strokeWidthProperty = SimpleDoubleProperty(1.0)
    private val useZLayerViewDistance = SimpleBooleanProperty(true)
    private val centerPlayerCursor = SimpleBooleanProperty(true)
    private val showPoiProperty = SimpleBooleanProperty(true)
    private val showCursorHint = SimpleBooleanProperty(true)

    private val properties = PersistentProperties.load("config.json")

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

    @Bean(name = ["windowPosition"])
    fun windowPosition() = windowPosition

    @Bean(name = ["windowSize"])
    fun windowSize() = windowSize

    @Bean(name = ["transparency"])
    fun transparency() = transparency

    @Bean(name = ["falseColor"])
    fun falseColor() = falseColor

    @Bean(name = ["backgroundColor"])
    fun backgroundColor() = backgroundColor
}