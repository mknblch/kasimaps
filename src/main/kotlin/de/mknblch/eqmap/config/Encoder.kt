package de.mknblch.eqmap.config

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Cipher.*
import javax.crypto.spec.SecretKeySpec
import kotlin.random.nextUInt

class Encoder(password: String) {

    private val secretKey = SecretKeySpec(hash(password).toByteArray(), "AES")

    private val e: Cipher = getInstance("AES").also {
        it.init(ENCRYPT_MODE, secretKey)
    }
    private val d: Cipher = getInstance("AES").also {
        it.init(DECRYPT_MODE, secretKey)
    }

    fun encrypt(str: String): String {
        val random = randomString(4)
        val utf8Data = "$random$str".toByteArray(charset)
        return Base64.getEncoder().encodeToString(e.doFinal(utf8Data))
    }

    @Throws(Exception::class)
    fun decrypt(str: String): String? {
        return try {
            val string = String(d.doFinal(Base64.getDecoder().decode(str)), charset)
            string.removeRange(0, 4)
        } catch (e: Exception) {
            null
        }
    }

    companion object {

        private val charset = charset("UTF8")
        private val alphabet = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        private val md5 = MessageDigest.getInstance("MD5")

        private fun hash(clearText: String): String {
            val data: ByteArray = md5.digest(clearText.toByteArray(Charsets.UTF_8))
            return BigInteger(1, data).toString(16)
        }

        private fun randomString(length: Int): String =
            (0 until length).map { alphabet.random() }.joinToString("")
    }
}