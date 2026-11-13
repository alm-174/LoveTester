package com.example.lovetester.ui.splash

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentSplashBinding

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var animator: ValueAnimator? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentSplashBinding.bind(view)

        val dots = listOf(
            binding.dot1,
            binding.dot2,
            binding.dot3,
            binding.dot4
        )

        animator = ValueAnimator.ofInt(0, 4).apply {
            duration = 1200L
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                val activeIndex = value.coerceAtMost(3)

                dots.forEachIndexed { index, dot ->
                    dot.setBackgroundResource(
                        if (index == activeIndex)
                            R.drawable.bg_splash_dot
                        else
                            R.drawable.bg_splash_dot_inactive
                    )
                }
            }
        }

        var wasCancelled = false

        animator?.doOnCancel {
            wasCancelled = true
        }

        animator?.doOnEnd {
            if (!wasCancelled &&
                isAdded &&
                findNavController().currentDestination?.id == R.id.splashFragment
            ) {
                findNavController().navigate(
                    R.id.action_splash_to_language
                )
            }
        }

        animator?.start()
    }



    override fun onDestroyView() {
        animator?.cancel()
        animator = null
        super.onDestroyView()
    }
}