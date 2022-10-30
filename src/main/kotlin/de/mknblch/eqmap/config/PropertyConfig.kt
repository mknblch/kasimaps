package de.mknblch.eqmap.config

import de.mknblch.eqmap.common.PersistentProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PropertyConfig {

    private val properties = PersistentProperties.load("config.json").also {
        logger.debug("loaded ${it.data}")
    }

    @Bean
    fun properties(): PersistentProperties = properties

    companion object {
        private val logger = LoggerFactory.getLogger(PropertyConfig::class.java)
    }
}