package de.mknblch.eqmap.config

import javafx.stage.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class BeanConfiguration {

    @Bean
    fun resourceBundle(): ResourceBundle {
        return ResourceBundle.getBundle("bundle")
    }

}