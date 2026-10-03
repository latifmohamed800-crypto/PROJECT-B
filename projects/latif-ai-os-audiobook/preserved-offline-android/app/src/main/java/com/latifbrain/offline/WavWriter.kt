package com.latifbrain.offline

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.max
import kotlin.math.min

object WavWriter {
    fun floatToPcm16(samples: FloatArray, file: File) {
        require(samples.isNotEmpty()) { "No audio samples" }
        val partial = File(file.parentFile, file.name + ".partial")
        try {
            BufferedOutputStream(FileOutputStream(partial)).use { out ->
                val buffer = ByteArray(8192)
                var bi = 0
                for (sample in samples) {
                    val clamped = if (sample.isFinite()) max(-1f, min(1f, sample)) else 0f
                    val v = (clamped * 32767f).toInt().coerceIn(-32768, 32767)
                    buffer[bi++] = (v and 0xff).toByte()
                    buffer[bi++] = ((v ushr 8) and 0xff).toByte()
                    if (bi >= buffer.size - 2) {
                        out.write(buffer, 0, bi)
                        bi = 0
                    }
                }
                if (bi > 0) out.write(buffer, 0, bi)
            }
            check(partial.renameTo(file)) { "Could not commit audio segment" }
        } finally { partial.delete() }
    }

    fun mergePcm16Mono(segments: List<File>, output: File, sampleRate: Int, checkActive: () -> Unit = {}) {
        require(sampleRate in 8000..192000 && segments.isNotEmpty()) { "Invalid WAV input" }
        val size = segments.sumOf { require(it.length() > 0 && it.length() % 2 == 0L) { "Invalid PCM segment" }; it.length() }
        require(size <= 0xffffffffL - 36L) { "WAV exceeds 4 GB; render smaller chapters" }
        checkActive()
        output.parentFile?.mkdirs()
        val partial = File(output.parentFile, output.name + ".partial")
        try {
            RandomAccessFile(partial, "rw").use { wav ->
                wav.setLength(0)
                repeat(44) { wav.write(0) }
                var dataBytes = 0L
                val buffer = ByteArray(64 * 1024)
                for (segment in segments) {
                    segment.inputStream().buffered().use { input ->
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            checkActive()
                            wav.write(buffer, 0, n)
                            dataBytes += n
                        }
                    }
                }
                wav.seek(0)
                writeAscii(wav, "RIFF")
                writeLe32(wav, 36L + dataBytes)
                writeAscii(wav, "WAVE")
                writeAscii(wav, "fmt ")
                writeLe32(wav, 16)
                writeLe16(wav, 1)
                writeLe16(wav, 1)
                writeLe32(wav, sampleRate.toLong())
                writeLe32(wav, sampleRate.toLong() * 2)
                writeLe16(wav, 2)
                writeLe16(wav, 16)
                writeAscii(wav, "data")
                writeLe32(wav, dataBytes)
            }
            checkActive()
            check(partial.renameTo(output)) { "Could not commit WAV output" }
        } finally { partial.delete() }
    }

    private fun writeAscii(f: RandomAccessFile, s: String) = f.write(s.toByteArray(Charsets.US_ASCII))
    private fun writeLe16(f: RandomAccessFile, v: Int) {
        f.write(v and 0xff)
        f.write((v ushr 8) and 0xff)
    }
    private fun writeLe32(f: RandomAccessFile, v: Long) {
        f.write((v and 0xff).toInt())
        f.write(((v ushr 8) and 0xff).toInt())
        f.write(((v ushr 16) and 0xff).toInt())
        f.write(((v ushr 24) and 0xff).toInt())
    }
}
