package com.example.lovetester.ui.language

import com.example.lovetester.R

object SupportedLanguages {

    val all = listOf(
        Language("en", R.string.language_english, R.drawable.flag_english),
        Language("hi", R.string.language_hindi, R.drawable.flag_hindi),
        Language("es", R.string.language_spanish, R.drawable.flag_spanish),
        Language("fr", R.string.language_french, R.drawable.flag_french),
        Language("pt", R.string.language_portuguese, R.drawable.flag_portuguese),
        Language("id", R.string.language_indonesian, R.drawable.flag_indonesian),
        Language("de", R.string.language_german, R.drawable.flag_german)
    )
}