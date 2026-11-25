package com.example.lovetester.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * `private val binding by viewBinding(FragmentFooBinding::bind)`
 *
 * Binds to the fragment's view on first use and forgets it when the view is destroyed, so a
 * Fragment / BottomSheet / DialogFragment no longer needs `_binding = null` in onDestroyView.
 * Works for any fragment that has a view (layout passed to the constructor, or onCreateView).
 */
fun <T : ViewBinding> Fragment.viewBinding(bind: (View) -> T): ReadOnlyProperty<Fragment, T> =
    object : ReadOnlyProperty<Fragment, T>, DefaultLifecycleObserver {
        private var binding: T? = null

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            binding?.let { return it }
            val viewLifecycle = thisRef.viewLifecycleOwner.lifecycle
            val newBinding = bind(thisRef.requireView())
            // The view lifecycle reaches DESTROYED just before onDestroyView runs: still hand out a
            // binding there (for cleanup code), but do not keep it, or it would outlive the view.
            if (viewLifecycle.currentState != Lifecycle.State.DESTROYED) {
                binding = newBinding
                viewLifecycle.addObserver(this)
            }
            return newBinding
        }

        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            binding = null
        }
    }
