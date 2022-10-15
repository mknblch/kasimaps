package de.mknblch.eqmap.zone

import javafx.scene.Node
import kotlin.math.max
import kotlin.math.min

data class ZoneMap(
    val name: String,
    val shortName: String,
    val elements: List<MapNode>,
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

    fun pointInBounds(x: Double, y: Double): Boolean {
        return x in minX..maxX && y in minY..maxY
    }

    fun toTypedArray() = elements.map { it as Node }.toTypedArray()
}
