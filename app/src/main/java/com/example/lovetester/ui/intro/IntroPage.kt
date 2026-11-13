package com.example.lovetester.ui.intro

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes


data class IntroPage(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)