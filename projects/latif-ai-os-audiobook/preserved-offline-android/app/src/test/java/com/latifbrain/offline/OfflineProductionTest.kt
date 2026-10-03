package com.latifbrain.offline

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OfflineProductionTest {
    @Test fun collidingJavaStringsNeverShareRenderCache() {
        assertEquals("Aa".hashCode(), "BB".hashCode())
        assertNotEquals(RenderIdentity.key("Aa", 1f), RenderIdentity.key("BB", 1f))
        assertNotEquals(RenderIdentity.key("Aa", 1f), RenderIdentity.key("Aa", 1.1f))
    }
    @Test fun chunksPreserveSupplementaryUnicode() {
        val text = "😀".repeat(200)
        val chunks = BookChunker.split(text, 7)
        assertEquals(text, chunks.joinToString(""))
        assertTrue(chunks.all { it.length <= 7 && !it.last().isHighSurrogate() })
        assertThrows(IllegalArgumentException::class.java) { BookChunker.split(text, 1) }
    }
    @Test fun wavHeaderDescribesActualPcm() {
        val dir = Files.createTempDirectory("wav").toFile()
        try {
            val pcm = File(dir, "audio.pcm")
            val wav = File(dir, "audio.wav")
            WavWriter.floatToPcm16(floatArrayOf(0f, 1f, -1f, Float.NaN), pcm)
            WavWriter.mergePcm16Mono(listOf(pcm), wav, 24000)
            val bytes = wav.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", String(bytes, 0, 4))
            assertEquals(44, buffer.getInt(4))
            assertEquals(24000, buffer.getInt(24))
            assertEquals(8, buffer.getInt(40))
            assertEquals(52, bytes.size)
        } finally { dir.deleteRecursively() }
    }
    @Test fun cancelledAssemblyPreservesPreviousOutputAndRemovesPartial() {
        val dir = Files.createTempDirectory("wav-cancel").toFile()
        try {
            val pcm = File(dir, "audio.pcm").apply { writeBytes(ByteArray(200000)) }
            val wav = File(dir, "audio.wav").apply { writeText("previous complete output") }
            var checks = 0
            assertThrows(InterruptedException::class.java) {
                WavWriter.mergePcm16Mono(listOf(pcm), wav, 24000) { if (++checks > 2) throw InterruptedException() }
            }
            assertEquals("previous complete output", wav.readText())
            assertFalse(File(dir, "audio.wav.partial").exists())
        } finally { dir.deleteRecursively() }
    }
    @Test fun malformedPcmCannotProduceApparentlyValidWav() {
        val dir = Files.createTempDirectory("wav-bad").toFile()
        try {
            val pcm = File(dir, "odd.pcm").apply { writeBytes(byteArrayOf(1)) }
            assertThrows(IllegalArgumentException::class.java) { WavWriter.mergePcm16Mono(listOf(pcm), File(dir, "out.wav"), 24000) }
        } finally { dir.deleteRecursively() }
    }
    @Test fun manuscriptsRejectInvalidUtf8AndOversizedInput() {
        assertEquals("كتاب", ManuscriptText.read("كتاب".byteInputStream()))
        assertThrows(Exception::class.java) { ManuscriptText.read(byteArrayOf(0xc3.toByte(), 0x28).inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { ManuscriptText.read(ByteArray(ManuscriptText.MAX_BYTES + 1) { 65 }.inputStream()) }
    }
}
