package com.example.lovetester.ui.background

sealed interface BackgroundItem {

    data object AddPhoto : BackgroundItem

    data class BuiltIn(
        val id: String,
        val drawableRes: Int
    ) : BackgroundItem

    data class Custom(
        val path: String
    ) : BackgroundItem
}

