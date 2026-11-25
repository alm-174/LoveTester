package com.example.lovetester.data.local.model

import java.time.LocalDate

data class PersonInfo(
    val name: String,
    val gender: Gender,
    val birthday: LocalDate? = null,
    val avatarPath: String? = null
)

enum class Gender {
    MALE,
    FEMALE,
    OTHER
}

const val MAX_NAME_LENGTH = 100

/**
 * Developer guard: the UI must check the name BEFORE calling this and show its own
 * localized message. The messages below are for logs only, never shown to the user.
 */
fun validateName(raw: String): String {
    val title = raw.trim()
    require(title.isNotBlank()) { "Name must not be blank" }
    require(title.length <= MAX_NAME_LENGTH) { "Name must be at most $MAX_NAME_LENGTH characters" }
    return title
}