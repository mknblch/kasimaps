package de.mknblch.eqmap.zone

data class MapLayer(
    val name: String,
    val nodes: List<MapNode>,
    var show: Boolean = true
)

val LayerComparator: Comparator<MapLayer> = Comparator.comparingInt<MapLayer> { it.name.length }.thenComparing { it -> it.name }