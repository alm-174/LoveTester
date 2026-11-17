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

fun validateName(raw: String): String {
    val title = raw.trim()
    require(title.isNotBlank()) { "Tên không được trống" }
    require(title.length <= 100) { "Tên tối đa 100 ký tự" }
    return title
}