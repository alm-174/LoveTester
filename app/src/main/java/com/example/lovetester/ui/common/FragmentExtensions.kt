package com.example.lovetester.ui.common

import androidx.annotation.DimenRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Shows [dialog] unless one with the same [tag] is already showing (or about to show).
 * Uses showNow so the fragment is registered immediately: a fast double tap cannot open two.
 */
fun FragmentManager.showOnce(dialog: DialogFragment, tag: String) {
    if (isStateSaved || findFragmentByTag(tag) != null) return
    dialog.showNow(this, tag)
}

/**
 * Opens the sheet fully, never taller than [maxHeightRes]. The layout wraps its content, so on a
 * small screen the sheet is limited by the screen instead and the list inside scrolls.
 * Call from onStart (the dialog's behavior exists from then on).
 */
fun BottomSheetDialogFragment.expandWithMaxHeight(@DimenRes maxHeightRes: Int) {
    val behavior = (dialog as? BottomSheetDialog)?.behavior ?: return
    behavior.maxHeight = resources.getDimensionPixelSize(maxHeightRes)
    behavior.skipCollapsed = true
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
}
