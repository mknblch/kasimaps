package de.mknblch.eqmap.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.shadow.com.univocity.parsers.common.fields.ColumnMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class ZoneNameConfigTest {

    @Autowired
    @Qualifier("zoneMapping")
    private lateinit var zoneMapping: Map<String, String>

    @Test
    fun testMap() {

        zoneMapping.forEach {
            println(it)
        }

        assertEquals("soldungb", zoneMapping["nagafen's lair"])
    }
}