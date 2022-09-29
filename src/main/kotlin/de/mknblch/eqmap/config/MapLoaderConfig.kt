package de.mknblch.eqmap.config

import de.mknblch.eqmap.map.MapLine
import de.mknblch.eqmap.map.MapObject
import javafx.scene.Group
import javafx.scene.Node
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min

typealias MapLayer = Pair<String, List<MapObject>>

data class ZoneMap(
    val name: String,
    val elements: List<MapObject>,
    val layer: List<MapLayer>
) {

    val minX: Double by lazy {
        elements.filterIsInstance<MapLine>().minOf { min(it.x1, it.x2) }
    }

    val maxX: Double by lazy {
        elements.filterIsInstance<MapLine>().maxOf { max(it.x1, it.x2) }
    }

    val minY: Double by lazy {
        elements.filterIsInstance<MapLine>().minOf { min(it.y1, it.y2) }
    }

    val maxY: Double by lazy {
        elements.filterIsInstance<MapLine>().maxOf { max(it.y1, it.y2) }
    }

    val minZ: Double by lazy {
        elements.filterIsInstance<MapLine>().minOf { min(it.z1, it.z2) }
    }

    val maxZ: Double by lazy {
        elements.filterIsInstance<MapLine>().maxOf { max(it.z1, it.z2) }
    }

    fun toTypedArray() = elements.map { it as Node }.toTypedArray()
}

@Configuration
class MapLoaderConfig {

    @Value("classpath*:p99/*.txt")
    private lateinit var resources: Array<Resource>

    @Qualifier("fileMapping")
    @Autowired
    private lateinit var fileMapping: Map<String, String>

    @OptIn(DelicateCoroutinesApi::class)
    @Bean
    fun loadZoneMaps(): List<ZoneMap> {
        val jobs = resources.groupBy {
            it.filename?.split(".", "_")?.get(0)?.lowercase()
        }.mapNotNull { entry ->
            if (entry.key == null) return@mapNotNull null
            GlobalScope.async(Dispatchers.IO) {
                val layer: List<MapLayer> = entry.value.mapNotNull(::loadLayer)
                val elements: List<MapObject> = layer.flatMap { it.second }
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
                        .mapNotNull(MapObject::fromString)
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