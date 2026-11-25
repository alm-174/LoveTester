package com.example.lovetester.ui.common

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView

/** Hides the soft keyboard (if shown) and drops focus from this view. */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
    clearFocus()
}

/** Runs [action] (default: hide the keyboard) when the user taps "Done" on the keyboard. */
fun TextView.onImeDone(action: () -> Unit = { hideKeyboard() }) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            action()
            true
        } else {
            false
        }
    }
}
