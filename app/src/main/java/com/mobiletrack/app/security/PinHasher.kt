package com.mobiletrack.app.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PinHasher {
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(Base64.decode(salt, Base64.NO_WRAP))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun verify(pin: String, expectedHash: String, salt: String): Boolean =
        hash(pin, salt) == expectedHash
}
