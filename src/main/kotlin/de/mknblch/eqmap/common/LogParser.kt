package de.mknblch.eqmap.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

abstract class LogParser(val logfile: File) : AutoCloseable {

    private val active = AtomicBoolean(true)
    private var randomAccessFile: RandomAccessFile = RandomAccessFile(logfile, "r")

    init {

        GlobalScope.async(Dispatchers.IO) {
            logger.debug("starting parser $logfile")
            readLoop()
            randomAccessFile.close()
            logger.debug("$logfile closed")
        }
    }

    private suspend fun readLoop() {
        var chr: Int
        var cr = false
        val builder = StringBuffer()
        var index = randomAccessFile.length()
        randomAccessFile.seek(index)

        Loop@ while (active.get()) {
            if (randomAccessFile.length() < index) {
                index = randomAccessFile.length()
                randomAccessFile.seek(index)
            }
            chr = randomAccessFile.read()
            index++
            when {
                // wait for input
                chr == -1 -> {
                    delay(1)
                    continue@Loop
                }
                // emit complete sentence
                chr == NL && cr -> {
                    emit(builder.toString())
                    builder.setLength(0)
                    cr = false
                }
                // set CR
                chr == CR -> cr = true
                // append
                else -> {
                    builder.append(chr.toChar())
                    cr = false
                }
            }
        }
        logger.debug("read loop ended")
    }

    abstract fun emit(value: String)

    override fun close() {
        active.set(false)
    }

    companion object {
        private const val NL = '\n'.code
        private const val CR = '\r'.code
        private val logger: Logger = LoggerFactory.getLogger(LogParser::class.java)
    }
}