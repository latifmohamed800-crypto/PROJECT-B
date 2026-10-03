package com.latifbrain.offline

import java.io.InputStream
import java.nio.ByteBuffer

object ManuscriptText {
    const val MAX_BYTES = 5 * 1024 * 1024
    fun read(input: InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, MAX_BYTES + 1 - output.size()))
            if (count <= 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_BYTES) break
        }
        val bytes = output.toByteArray()
        require(bytes.size <= MAX_BYTES) { "Manuscript exceeds 5 MB. Import a smaller chapter." }
        val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF").trim()
        require(text.isNotBlank() && !text.contains('\u0000')) { "Choose a nonempty UTF-8 text manuscript." }
        return text
    }
}
