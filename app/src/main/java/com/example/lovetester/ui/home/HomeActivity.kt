package com.example.lovetester.ui.home

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovetester.R
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivityHomeBinding
import com.example.lovetester.ui.background.BackgroundBottomSheet
import com.example.lovetester.ui.startdate.StartDateFragment
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        binding.btnGallery.setOnClickListener {
            BackgroundBottomSheet().show(
                supportFragmentManager,
                "background_sheet"
            )
        }

        binding.layoutLoveDays.setOnClickListener {
            openStartDate()
        }

        val repository = LoveSettingsRepository(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.loveStartDate.collect { startDate ->

                    if (startDate == null) {
                        binding.tvLoveDays.text = "0 Day"
                    } else {
                        val days = ChronoUnit.DAYS.between(
                            startDate,
                            LocalDate.now()
                        ).coerceAtLeast(0)

                        binding.tvLoveDays.text = "$days\nDay"
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.selectedBackgroundId.collect { id ->

                    when {
                        id?.startsWith("custom:") == true -> {
                            val path = id.removePrefix("custom:")

                            binding.ivHomeBackground.setImageURI(null)
                            binding.ivHomeBackground.setImageURI(
                                Uri.fromFile(File(path))
                            )
                        }

                        else -> {
                            val drawableRes = when (id) {
                                "romantic_1" -> R.drawable.background_romantic_1
                                "romantic_2" -> R.drawable.background_romantic_2
                                "romantic_3" -> R.drawable.background_romantic_3
                                else -> R.drawable.home_background
                            }

                            binding.ivHomeBackground.setImageResource(drawableRes)
                        }
                    }
                }
            }
        }

    }

    private fun applyWindowInsets() {
        val settingsParams =
            binding.btnSettings.layoutParams as ViewGroup.MarginLayoutParams
        val initialSettingsTop = settingsParams.topMargin
        val initialSettingsStart = settingsParams.marginStart

        val galleryParams =
            binding.btnGallery.layoutParams as ViewGroup.MarginLayoutParams
        val initialGalleryTop = galleryParams.topMargin
        val initialGalleryEnd = galleryParams.marginEnd

        val bottomNavParams =
            binding.layoutBottomNav.layoutParams as ViewGroup.MarginLayoutParams
        val initialBottomNavStart = bottomNavParams.marginStart
        val initialBottomNavEnd = bottomNavParams.marginEnd
        val initialBottomNavBottom = bottomNavParams.bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val navigationInsets = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )

            binding.btnSettings.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = maxOf(initialSettingsTop, statusInsets.top)
                marginStart = initialSettingsStart + statusInsets.left
            }

            binding.btnGallery.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = maxOf(initialGalleryTop, statusInsets.top)
                marginEnd = initialGalleryEnd + statusInsets.right
            }

            binding.layoutBottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottomNavBottom + navigationInsets.bottom
                marginStart = initialBottomNavStart + navigationInsets.left
                marginEnd = initialBottomNavEnd + navigationInsets.right
            }

            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun openStartDate() {

        binding.startDateContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.startDateContainer,
                StartDateFragment()
            )
            .addToBackStack("start_date")
            .commit()
    }
}