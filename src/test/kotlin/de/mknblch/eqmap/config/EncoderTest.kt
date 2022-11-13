package de.mknblch.eqmap.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.random.nextULong

internal class EncoderTest {

    @Test
    fun testEncode() {
        val encoder = Encoder(Encoder.randomString(6))
        val clearText = Random.nextULong().toString(26)
        var last: String? = null
        for (i in 0 until 1000) {
            val encrypt = encoder.encrypt(clearText)
            assertNotEquals(last, encrypt)
            last = encrypt
            val decrypt = encoder.decrypt(encrypt) ?: fail()
            assertEquals(clearText, decrypt)
        }
    }
}