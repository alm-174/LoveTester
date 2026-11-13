package com.example.lovetester.ui.language

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class Language(
    val id: String,
    @StringRes val nameRes: Int,
    @DrawableRes val flagRes: Int
)