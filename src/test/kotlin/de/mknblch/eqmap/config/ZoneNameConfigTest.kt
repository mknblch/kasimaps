package de.mknblch.eqmap.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestAppStarter::class])
internal class ZoneNameConfigTest {

    @Autowired
    @Qualifier("zoneMapping")
    private lateinit var zoneMapping: Map<String, String>

    @Test
    fun testMap() {

        zoneMapping.forEach {
            println(it)
        }

//        assertEquals("soldungb", zoneMapping["nagafen's lair"])
    }
}