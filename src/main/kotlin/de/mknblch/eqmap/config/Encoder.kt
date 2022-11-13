package de.mknblch.eqmap.config

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Cipher.*
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random


class Encoder(password: String) {

    private val secretKey = SecretKeySpec(md5(password).padEnd(32, '=').toByteArray(), "AES")

    private val e: Cipher = getInstance("AES").also {
        it.init(ENCRYPT_MODE, secretKey)
    }
    private val d: Cipher = getInstance("AES").also {
        it.init(DECRYPT_MODE, secretKey)
    }

    fun encrypt(str: String): String {
        val length = Random.nextInt(2, 9)
        val random = randomString(length)
        val utf8Data = (length.toString() + random + str.trim().xor(random)).toByteArray(Charsets.UTF_8)
        return Base64.getEncoder().encodeToString(e.doFinal(utf8Data))
    }

    @Throws(Exception::class)
    fun decrypt(str: String): String? {
        return try {
            val string = String(d.doFinal(Base64.getDecoder().decode(str)), Charsets.UTF_8)
            val length = string.substring(0, 1).toIntOrNull() ?: return null
            val random = string.substring(1, length + 1)
            string.removeRange(0, length + 1).xor(random).trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun String.xor(password: String): String = this.mapIndexed { index, c ->
        c.code xor password[index % password.length].code
    }.map { Char(it) }.joinToString("")

    companion object {

        private val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val md5 = MessageDigest.getInstance("MD5")

        fun md5(clearText: String): String {
            val data: ByteArray = md5.digest(clearText.toByteArray(Charsets.UTF_8))
            return BigInteger(1, data).toString(16)
        }

        fun randomString(length: Int): String =
            (0 until length).map { alphabet.random() }.joinToString("")
    }
}