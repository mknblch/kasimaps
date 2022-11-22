package de.mknblch.eqmap.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestAppStarter::class])
internal class ZoneNameConfigTest {

    @Autowired
    @Qualifier("zoneMapping")
    private lateinit var zoneMapping: Map<String, String>

    @Autowired
    @Qualifier("whoMapping")
    private lateinit var whoMapping: Map<String, String>

    @Test
    fun testMap() {
        zoneMapping.forEach(::println)
        assertEquals("soldungb", zoneMapping["nagafen's lair"])
    }

    @Test
    fun testWho() {
        whoMapping.forEach(::println)
        assertEquals("kithicor", zoneMapping["kithicor woods"])
    }
}