package com.example.lovetester.ui.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.R
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivitySplashBinding
import com.example.lovetester.ui.home.HomeActivity
import com.example.lovetester.ui.intro.IntroActivity
import com.example.lovetester.ui.language.LanguageActivity
import com.example.lovetester.ui.permission.PermissionActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var animator: ValueAnimator? = null
    private var animationFinished = false
    private var onboardingStep: OnboardingStep? = null
    private var activityResumed = false
    private var completionHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        binding.root.setBackgroundResource(R.drawable.bg_splash)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)
        ViewCompat.requestApplyInsets(binding.root)

        loadOnboardingStep()
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        var wasCancelled = false

        animator = ValueAnimator.ofInt(0, 4).apply {
            duration = 1200L
            interpolator = LinearInterpolator()
            repeatCount = 0

            addUpdateListener { animation ->
                val activeIndex = (animation.animatedValue as Int).coerceAtMost(3)

                dots.forEachIndexed { index, dot ->
                    dot.setBackgroundResource(
                        if (index == activeIndex) {
                            R.drawable.bg_splash_dot
                        } else {
                            R.drawable.bg_splash_dot_inactive
                        }
                    )
                }
            }

            doOnCancel {
                wasCancelled = true
            }
            doOnEnd {
                if (!wasCancelled) {
                    animationFinished = true
                    navigateIfReady()
                }
            }
        }
        animator?.start()
    }

    override fun onPostResume() {
        super.onPostResume()
        activityResumed = true
        navigateIfReady()
    }

    override fun onPause() {
        activityResumed = false
        super.onPause()
    }

    private fun loadOnboardingStep() {
        val repository = LoveSettingsRepository(applicationContext)

        lifecycleScope.launch {
            onboardingStep = try {
                repository.onboardingStep.first()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                OnboardingStep.LANGUAGE
            }

            navigateIfReady()
        }
    }

    private fun navigateIfReady() {
        val step = onboardingStep ?: return

        if (!animationFinished || !activityResumed || completionHandled || isFinishing || isDestroyed) {
            return
        }
        completionHandled = true

        val destination = when (step) {
            OnboardingStep.LANGUAGE -> LanguageActivity::class.java
            OnboardingStep.INTRO -> IntroActivity::class.java
            OnboardingStep.PERMISSION -> PermissionActivity::class.java
            OnboardingStep.COMPLETED -> HomeActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }

    override fun onDestroy() {
        activityResumed = false
        animator?.cancel()
        animator?.removeAllUpdateListeners()
        animator?.removeAllListeners()
        animator = null
        super.onDestroy()
    }
}
