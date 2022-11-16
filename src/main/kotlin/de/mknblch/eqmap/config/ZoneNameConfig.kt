package de.mknblch.eqmap.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.InputStreamReader

@Configuration
class ZoneNameConfig {

    private val regex = Regex("([^=]+)=(.+)")

    @Value("classpath:/map_keys.ini")
    private lateinit var mapKeys: Resource

    @Value("classpath:/map_keys_who.ini")
    private lateinit var mapKeysWho: Resource

    @Qualifier("zoneMapping")
    @Bean
    fun zoneMapping(): Map<String, String> {
        // maps: sirens groto -> sirens
        val rawKey = InputStreamReader(mapKeys.inputStream).readLines().mapNotNull {
            val groups = regex.matchEntire(it)?.groupValues ?: return@mapNotNull null
            Pair(groups[1].trim(), groups[2].trim())
        }.toMap()

        return rawKey
    }

    @Qualifier("fileMapping")
    @Bean
    fun fileMapping(): Map<String, String> = zoneMapping()
        .map {
            Pair(it.value, it.key)
        }.toMap()

    @Qualifier("whoMapping")
    @Bean
    fun whoMapping(): Map<String, String> {

        val zoneMapping = zoneMapping()

        val whoMapping = InputStreamReader(mapKeysWho.inputStream).readLines().mapNotNull {
            val groups = regex.matchEntire(it)?.groupValues ?: return@mapNotNull null
            Pair(groups[1].trim(), groups[2].trim())
        }.toMap().mapValues {
            zoneMapping[it.value] ?: it.value
        }

        return zoneMapping + whoMapping
    }
}