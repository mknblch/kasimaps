package de.mknblch.eqmap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class ResourceConfig {

    @Bean
    fun resourceBundle(): ResourceBundle {
        return ResourceBundle.getBundle("bundle")
    }

}