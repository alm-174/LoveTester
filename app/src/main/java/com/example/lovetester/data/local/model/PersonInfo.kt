package com.example.lovetester.data.local.model

data class PersonInfo(
    val name: String,
    val gender: Gender
)

enum class Gender {
    MALE,
    FEMALE
}