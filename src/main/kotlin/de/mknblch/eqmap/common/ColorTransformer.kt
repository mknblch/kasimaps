package de.mknblch.eqmap.common

import de.mknblch.eqmap.common.ColorTransformer.Companion.generatePalette
import de.mknblch.eqmap.zone.MapLine
import de.mknblch.eqmap.zone.MapNode
import de.mknblch.eqmap.zone.MapPOI
import javafx.scene.paint.Color
import kotlin.math.min

fun Color.withAlpha(alpha: Double): Color = Color(red, green, blue, alpha)

interface ColorTransformer {

    fun apply(objects: Collection<MapNode>)

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

        fun generateMonochromePalette(
            size: Int = 6,
        ): List<Color> {

            val d = 1.0 / (size - 1)
            return (0 until size).map {
                val v = (d * it).coerceAtMost(1.0)
                Color.color(v, v, v)
            }
        }
    }
}

object OriginalTransformer : ColorTransformer {
    override fun apply(objects: Collection<MapNode>) {
        objects.forEach {
            it.resetColor()
        }
    }
}

class ZColorTransformer(paletteSize: Int = 36) : ColorTransformer {

    private val palette = generatePalette(paletteSize)

    override fun apply(objects: Collection<MapNode>) {
        if (objects.isEmpty()) {
            return
        }
        val minZ: Double =
            objects.filterIsInstance<MapLine>().minOfOrNull { min(it.zRange.start, it.zRange.endInclusive) } ?: return
        val maxZ: Double =
            objects.filterIsInstance<MapLine>().maxOfOrNull { min(it.zRange.start, it.zRange.endInclusive) } ?: return
        objects.forEach {
            val v = (palette.size - 1) * (min(it.zRange.start, it.zRange.endInclusive) - minZ) / (maxZ - minZ)
            val color = palette[v.toInt().coerceIn(0, palette.size - 1)]
            it.setViewColor(color)
        }
    }

}

class POIDeriveColorTransformer(val color: Color) : ColorTransformer {

    override fun apply(objects: Collection<MapNode>) {
        objects.forEach { node ->
            if (node is MapPOI) {
                node.getViewColor()?.also {
                    node.setViewColor(
                        it.deriveColor(color.hue, color.saturation, color.brightness, color.opacity)
                    )
                }
            }
        }
    }

}
