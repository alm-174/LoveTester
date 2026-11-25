package com.example.lovetester.ui.common

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * Shows [dialog] unless one with the same [tag] is already showing (or about to show).
 * Uses showNow so the fragment is registered immediately: a fast double tap cannot open two.
 */
fun FragmentManager.showOnce(dialog: DialogFragment, tag: String) {
    if (isStateSaved || findFragmentByTag(tag) != null) return
    dialog.showNow(this, tag)
}
