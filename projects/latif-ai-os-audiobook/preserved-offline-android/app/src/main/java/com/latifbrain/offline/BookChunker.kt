package com.latifbrain.offline

object BookChunker {
    private val boundary = Regex("(?<=[.!?؟؛:])\\s+|\\n+")

    fun split(text: String, maxChars: Int = 280): List<String> {
        require(maxChars >= 2) { "Chunk size must be at least two" }
        val normalized = text.replace(Regex("[\\t ]+"), " ").trim()
        if (normalized.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        val current = StringBuilder()
        val sentences = normalized.split(boundary).map { it.trim() }.filter { it.isNotEmpty() }

        fun flush() {
            val s = current.toString().trim()
            if (s.isNotEmpty()) out += s
            current.clear()
        }

        for (sentence in sentences) {
            if (sentence.length > maxChars) {
                flush()
                var rest = sentence
                while (rest.length > maxChars) {
                    val candidate = rest.take(maxChars)
                    val cut = maxOf(candidate.lastIndexOf('،'), candidate.lastIndexOf(' '), candidate.lastIndexOf('؛'))
                    var safeCut = if (cut >= maxChars / 2) cut else maxChars
                    if (safeCut < rest.length && rest[safeCut - 1].isHighSurrogate()) safeCut--
                    out += rest.take(safeCut).trim()
                    rest = rest.drop(safeCut).trim()
                }
                if (rest.isNotEmpty()) current.append(rest)
            } else if (current.isEmpty()) {
                current.append(sentence)
            } else if (current.length + 1 + sentence.length <= maxChars) {
                current.append(' ').append(sentence)
            } else {
                flush()
                current.append(sentence)
            }
        }
        flush()
        return out
    }
}
