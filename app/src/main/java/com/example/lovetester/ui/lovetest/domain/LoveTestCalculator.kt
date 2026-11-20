package com.example.lovetester.ui.lovetest.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object LoveTestCalculator {

    fun calculateNameTest(
        firstName: String,
        secondName: String
    ): Int {

        val names = listOf(
            firstName.trim().lowercase(Locale.ROOT),
            secondName.trim().lowercase(Locale.ROOT)
        ).sorted()

        val input = names.joinToString("|") {
            "${it.length}:$it"
        }

        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))

        val value =
            ((digest[0].toInt() and 0xFF) shl 8) or
                    (digest[1].toInt() and 0xFF)

        return value % 101
    }
}