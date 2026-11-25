package com.example.lovetester.ui.common

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Collects this flow while [owner] is at least [state]; stops when it drops below and restarts
 * when it comes back. In a Fragment pass `viewLifecycleOwner`, not the fragment itself.
 */
fun <T> Flow<T>.collectOn(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
): Job = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(state) {
        collect { action(it) }
    }
}

fun Context.toast(@StringRes messageRes: Int) {
    Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
}
