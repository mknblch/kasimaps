package de.mknblch.eqmap.common

import de.mknblch.eqmap.common.ColorTransformer.Companion.colorizeNode
import de.mknblch.eqmap.common.ColorTransformer.Companion.generatePalette
import de.mknblch.eqmap.map.MapLine
import de.mknblch.eqmap.map.MapObject
import de.mknblch.eqmap.map.MapPOI
import javafx.scene.paint.Color
import kotlin.math.min

interface ColorTransformer {

    fun apply(objects: Collection<MapObject>)

    companion object {

        fun generatePalette(
            size: Int = 36,
            offset: Double = 0.0,
            width: Double = 360.0,
            saturation: Double = 0.9,
            brightness: Double = 0.9,
        ): List<Color> {

            val d = width / size
            return (0 until size).map { i ->
                Color.hsb(((i * d) + offset) % width, saturation, brightness)
            }
        }

        fun colorizeNode(mapObject: MapObject, color: Color) {
            when (mapObject) {
                is MapLine -> mapObject.stroke = color
                is MapPOI -> mapObject.text.fill = color
            }
        }
    }
}

object OriginalTransformer : ColorTransformer {
    override fun apply(objects: Collection<MapObject>) {
        objects.forEach {
            colorizeNode(it, it.color)
        }
    }
}

class ZColorTransformer(
    paletteSize: Int = 36
) : ColorTransformer {

    private val palette = generatePalette(paletteSize)

    override fun apply(objects: Collection<MapObject>) {
        val minZ: Double = objects.filterIsInstance<MapLine>().minOf { min(it.zRange.start, it.zRange.endInclusive) }
        val maxZ: Double = objects.filterIsInstance<MapLine>().maxOf { min(it.zRange.start, it.zRange.endInclusive) }
        objects.forEach {
            val v = (palette.size - 1) * (min(it.zRange.start, it.zRange.endInclusive) - minZ) / (maxZ - minZ)
            val color = palette[v.toInt().coerceIn(0, palette.size - 1)]
            colorizeNode(it, color)
        }
    }

}

