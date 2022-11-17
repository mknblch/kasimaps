package de.mknblch.eqmap.zone

interface POI {

    val name: String

    fun getSeparateNames(): List<String> = name.split('|')

}