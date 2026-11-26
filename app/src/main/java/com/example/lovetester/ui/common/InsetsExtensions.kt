package com.example.lovetester.ui.common

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Edge-to-edge for screens with a LIGHT background: transparent bars with dark icons.
 * (Home keeps its own style because its top is a photo.)
 */
fun ComponentActivity.enableLightEdgeToEdge() {
    val light = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = light, navigationBarStyle = light)
}

/**
 * Adds the system bars (and display cutout) insets to this view's padding, on top of the padding
 * declared in XML. Needed on every screen since targetSdk 35 enforces edge-to-edge.
 */
fun View.applySystemBarsPadding() {
    val startLeft = paddingLeft
    val startTop = paddingTop
    val startRight = paddingRight
    val startBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(
            startLeft + bars.left,
            startTop + bars.top,
            startRight + bars.right,
            startBottom + bars.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applySystemBarsAndImePadding() {
    val startLeft = paddingLeft
    val startTop = paddingTop
    val startRight = paddingRight
    val startBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val safe = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
        )
        view.setPadding(
            startLeft + safe.left,
            startTop + safe.top,
            startRight + safe.right,
            startBottom + safe.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
