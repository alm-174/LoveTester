package com.example.lovetester.ui.home

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivityHomeBinding
import com.example.lovetester.ui.background.BackgroundBottomSheet
import com.example.lovetester.ui.information.InformationBottomSheet
import com.example.lovetester.ui.information.PersonType
import com.example.lovetester.ui.setting.SettingsActivity
import com.example.lovetester.ui.startdate.StartDateFragment
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.Period
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

        binding.layoutPersonOne.setOnClickListener {
            openInformation(PersonType.YOU)
        }

        binding.layoutPersonTwo.setOnClickListener {
            openInformation(PersonType.PARTNER)
        }


        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.youInfo.collect { info ->
                    binding.tvPersonOneName.text = info.name

                    binding.ivPersonOneGender.setImageResource(
                        genderIcon(info.gender)
                    )

                    binding.tvPersonOneAge.text = calculateAge(info.birthday)

                    renderAvatar(
                        binding.ivPersonOne,
                        info.avatarPath,
                        R.drawable.avatar_sasuke
                    )
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.partnerInfo.collect { info ->
                    binding.tvPersonTwoName.text = info.name

                    binding.ivPersonTwoGender.setImageResource(
                        genderIcon(info.gender)
                    )

                    binding.tvPersonTwoAge.text = calculateAge(info.birthday)

                    renderAvatar(
                        binding.ivPersonTwo,
                        info.avatarPath,
                        R.drawable.avatar_naruto
                    )
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

    private fun genderIcon(gender: Gender): Int {
        return when (gender) {
            Gender.MALE -> R.drawable.male
            Gender.FEMALE -> R.drawable.female
            Gender.OTHER -> R.drawable.other_gender
        }
    }

    private fun openInformation(personType: PersonType) {
        InformationBottomSheet
            .newInstance(personType)
            .show(
                supportFragmentManager,
                "information_sheet"
            )
    }

    private fun calculateAge(birthday: LocalDate?): String {
        if (birthday == null) {
            return "--"
        }

        val today = LocalDate.now()

        if (birthday.isAfter(today)) {
            return "--"
        }

        return Period.between(birthday, today).years.toString()
    }

    private fun renderAvatar(
        imageView: ImageView,
        path: String?,
        defaultDrawable: Int
    ) {
        val file = path?.let { File(it) }

        if (file != null && file.isFile) {
            imageView.setImageURI(Uri.fromFile(file))
        } else {
            imageView.setImageResource(defaultDrawable)
        }
    }
}