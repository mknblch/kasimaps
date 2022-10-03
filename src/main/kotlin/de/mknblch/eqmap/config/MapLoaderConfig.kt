package de.mknblch.eqmap.config

import de.mknblch.eqmap.zone.MapLayer
import de.mknblch.eqmap.zone.MapLine
import de.mknblch.eqmap.zone.MapNode
import de.mknblch.eqmap.zone.ZoneMap
import javafx.scene.Node
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.Resource
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min


@Configuration
class MapLoaderConfig {

    @Value("classpath*:p99/*.txt")
    private lateinit var resources: Array<Resource>

    @Qualifier("fileMapping")
    @Autowired
    private lateinit var fileMapping: Map<String, String>

    @OptIn(DelicateCoroutinesApi::class)
    @Lazy
    @Bean
    fun loadZoneMaps(): List<ZoneMap> {
        val jobs = resources.groupBy {
            it.filename?.split(".", "_")?.get(0)?.lowercase()
        }.mapNotNull { entry ->
            if (entry.key == null) return@mapNotNull null
            GlobalScope.async(Dispatchers.IO) {
                val layer: List<MapLayer> = entry.value.mapNotNull(::loadLayer)
                val elements: List<MapNode> = layer.flatMap { it.nodes }
                val zoneName = fileMapping.getOrDefault(entry.key!!, entry.key!!)
                ZoneMap(zoneName, elements, layer)
            }
        }
        return runBlocking {
            jobs.awaitAll().sortedBy { it.name }
        }
    }

    companion object {

        private fun loadLayer(resource: Resource): MapLayer? {
            return try {
                logger.debug("loading ${resource.filename}")
                val list = resource.inputStream.use { stream ->
                    InputStreamReader(stream).readLines().filter(String::isNotBlank)
                        .mapNotNull(MapNode::fromString)
                        .distinct()
                }
                MapLayer(resource.filename ?: "null", list)
            } catch (e: Exception) {
                logger.error("error loading ${resource.filename}", e)
                null
            }
        }

        private val logger = LoggerFactory.getLogger(MapLoaderConfig::class.java)
    }
}