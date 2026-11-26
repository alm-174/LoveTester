package com.example.lovetester.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import coil3.load
import coil3.request.error
import com.example.lovetester.R
import com.example.lovetester.data.local.model.Gender
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.FragmentHomeBinding
import com.example.lovetester.ui.background.BackgroundBottomSheet
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.common.viewBinding
import com.example.lovetester.ui.information.InformationBottomSheet
import com.example.lovetester.ui.information.PersonType
import com.example.lovetester.ui.setting.SettingsActivity
import com.example.lovetester.ui.startdate.StartDateActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)

    @Inject
    lateinit var repository: LoveSettingsRepository

    private var isNavigating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyTopInsets()

        binding.btnGallery.setOnClickListener {
            childFragmentManager.showOnce(BackgroundBottomSheet(), SHEET_TAG)
        }

        binding.layoutLoveDays.setOnClickListener {
            if (isNavigating) return@setOnClickListener
            isNavigating = true
            startActivity(Intent(requireContext(), StartDateActivity::class.java))
        }

        binding.layoutPersonOne.setOnClickListener {
            openInformation(PersonType.YOU)
        }

        binding.layoutPersonTwo.setOnClickListener {
            openInformation(PersonType.PARTNER)
        }

        binding.btnSettings.setOnClickListener {
            if (isNavigating) return@setOnClickListener
            isNavigating = true
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        observeSettings()
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    private fun observeSettings() {
        repository.loveStartDate.collectOn(viewLifecycleOwner) { startDate ->
            if (startDate == null) {
                binding.tvLoveDays.text = getString(R.string.home_love_days_empty)
            } else {
                val days = ChronoUnit.DAYS.between(startDate, LocalDate.now()).coerceAtLeast(0)
                binding.tvLoveDays.text = resources.getQuantityString(
                    R.plurals.home_love_days,
                    days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    days
                )
            }
        }

        repository.selectedBackgroundId.collectOn(viewLifecycleOwner) { id ->
            when {
                id?.startsWith("custom:") == true -> {
                    val path = id.removePrefix("custom:")
                    binding.ivHomeBackground.load(File(path))
                }
                else -> {
                    val drawableRes = when (id) {
                        "romantic_1" -> R.drawable.background_romantic_1
                        "romantic_2" -> R.drawable.background_romantic_2
                        "romantic_3" -> R.drawable.background_romantic_3
                        else -> R.drawable.home_background
                    }
                    binding.ivHomeBackground.load(drawableRes)
                }
            }
        }

        repository.youInfo.collectOn(viewLifecycleOwner) { info ->
            binding.tvPersonOneName.text = info.name
            binding.ivPersonOneGender.setImageResource(genderIcon(info.gender))
            binding.tvPersonOneAge.text = calculateAge(info.birthday)
            renderAvatar(binding.ivPersonOne, info.avatarPath, R.drawable.avatar_sasuke)
        }

        repository.partnerInfo.collectOn(viewLifecycleOwner) { info ->
            binding.tvPersonTwoName.text = info.name
            binding.ivPersonTwoGender.setImageResource(genderIcon(info.gender))
            binding.tvPersonTwoAge.text = calculateAge(info.birthday)
            renderAvatar(binding.ivPersonTwo, info.avatarPath, R.drawable.avatar_naruto)
        }
    }

    private fun applyTopInsets() {
        val settingsParams = binding.btnSettings.layoutParams as ViewGroup.MarginLayoutParams
        val initialSettingsTop = settingsParams.topMargin
        val initialSettingsStart = settingsParams.marginStart

        val galleryParams = binding.btnGallery.layoutParams as ViewGroup.MarginLayoutParams
        val initialGalleryTop = galleryParams.topMargin
        val initialGalleryEnd = galleryParams.marginEnd

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.btnSettings.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = maxOf(initialSettingsTop, statusInsets.top)
                marginStart = initialSettingsStart + statusInsets.left
            }
            binding.btnGallery.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = maxOf(initialGalleryTop, statusInsets.top)
                marginEnd = initialGalleryEnd + statusInsets.right
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun openInformation(personType: PersonType) {
        childFragmentManager.showOnce(InformationBottomSheet.newInstance(personType), SHEET_TAG)
    }

    private fun genderIcon(gender: Gender): Int = when (gender) {
        Gender.MALE -> R.drawable.male
        Gender.FEMALE -> R.drawable.female
        Gender.OTHER -> R.drawable.other_gender
    }

    private fun calculateAge(birthday: LocalDate?): String {
        if (birthday == null) return "--"
        val today = LocalDate.now()
        if (birthday.isAfter(today)) return "--"
        return Period.between(birthday, today).years.toString()
    }

    private fun renderAvatar(imageView: ImageView, path: String?, defaultDrawable: Int) {
        val file = path?.let { File(it) }
        if (file != null && file.isFile) {
            imageView.load(file) { error(defaultDrawable) }
        } else {
            imageView.load(defaultDrawable)
        }
    }

    companion object {
        private const val SHEET_TAG = "home_sheet"
    }
}