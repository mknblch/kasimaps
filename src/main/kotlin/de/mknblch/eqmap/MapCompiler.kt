package de.mknblch.eqmap

import de.mknblch.eqmap.common.ColorTransformer.Companion.generatePalette
import de.mknblch.eqmap.config.ZoneMap
import de.mknblch.eqmap.map.MapLine
import de.mknblch.eqmap.map.MapPOI
import javafx.application.Application
import javafx.scene.paint.Color
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.io.File
import kotlin.math.max
import kotlin.math.min

//@SpringBootApplication
class MapCompiler: CommandLineRunner {

    @Autowired
    private lateinit var zones: List<ZoneMap>

    private val palette = generatePalette(180)

    override fun run(vararg args: String?) {

        zones.forEach { zone ->

            val minZ = zone.elements.filterIsInstance<MapLine>().minOf { min(it.zRange.start, it.zRange.endInclusive) }
            val maxZ = zone.elements.filterIsInstance<MapLine>().maxOf { max(it.zRange.start, it.zRange.endInclusive) }
            val d = maxZ - minZ

            zone.layer.forEach { layer ->
                val filename = layer.first
                val transformed = layer.second.mapNotNull {
                    when (it) {
                        is MapLine -> renderLine(it, minZ, maxZ)
                        is MapPOI -> renderPOI(it)
                        else -> null
                    }
                }.joinToString("\n")

                val file = File("./src/main/resources/test/$filename")
                file.outputStream().bufferedWriter().use {
                    it.write(transformed)
                }
                println("wrote ${transformed.length} byte to $filename")
            }
        }

    }

    // L -40.9281, 71.1260, 0.2000,  -40.9288, 82.9287, 0.2000,  0, 0, 0
    private fun renderLine(l: MapLine, minZ: Double, maxZ: Double): String {
        val color = transformColor(l.color, min(l.z1, l.z2), minZ, maxZ)
        return "L ${l.x1}, ${l.y1}, ${l.z1}, ${l.x2}, ${l.y2}, ${l.z2}, ${norm(color.red)}, ${norm(color.green)}, ${norm(color.blue)}"
    }

    // P -22.6081, 200.6916, -13.8416,  0, 0, 240,  3,  Zeek(Q)
    private fun renderPOI(p: MapPOI): String {
        return "P ${p.x}, ${p.y}, ${p.z}, ${norm(p.color.red)}, ${norm(p.color.green)}, ${norm(p.color.blue)}, ${p.type}, ${p.name}"
    }

    fun norm(d: Double): Int {
        return (d * 255).toInt()
    }

    private fun transformColor(color: Color, z: Double, minZ: Double, maxZ: Double): Color {
        val v = (palette.size - 1) * (z - minZ) / (maxZ - minZ)
        return palette[v.toInt()]
    }
}
//
fun main(args: Array<out String>) {
    SpringApplication.run(MapCompiler::class.java, *args)
}