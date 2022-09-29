package de.mknblch.eqmap.config

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PropertyConfig {

    private val zViewDistance = SimpleDoubleProperty(45.0)
    private val strokeWidthProperty = SimpleDoubleProperty(1.0)
    private val useZLayerViewDistance = SimpleBooleanProperty(true)
    private val centerPlayerCursor = SimpleBooleanProperty(true)
    private val showPoiProperty = SimpleBooleanProperty(true)

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
}