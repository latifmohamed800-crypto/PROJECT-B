package com.latifbrain.offline

import java.security.MessageDigest

object RenderIdentity {
    fun key(text: String, speed: Float): String {
        require(speed.isFinite() && speed > 0) { "Invalid narration speed" }
        val identity = "nabra-1.13.8-int8|chunks-280-v2|${speed.toRawBits()}|$text"
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
