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
import androidx.core.view.isVisible
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
import com.example.lovetester.ui.lovetest.LoveTestActivity
import com.example.lovetester.ui.memory.AddMemoryFragment
import com.example.lovetester.ui.memory.MemoryListFragment
import dagger.hilt.android.AndroidEntryPoint
import com.example.lovetester.ui.setting.SettingsActivity
import com.example.lovetester.ui.startdate.StartDateFragment
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

@AndroidEntryPoint
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

        supportFragmentManager.addOnBackStackChangedListener { renderMemoryNavigation() }
        renderMemoryNavigation()

        binding.layoutBottomNav.navHome.setOnClickListener { showHomeTab() }
        binding.layoutBottomNav.navMemory.setOnClickListener { showMemoryTab() }
        binding.layoutBottomNav.navLoveTest.setOnClickListener {
            startActivity(Intent(this, LoveTestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }

        val repository = LoveSettingsRepository(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.loveStartDate.collect { startDate ->

                    if (startDate == null) {
                        binding.tvLoveDays.text = getString(R.string.home_love_days_empty)
                    } else {
                        val days = ChronoUnit.DAYS.between(
                            startDate,
                            LocalDate.now()
                        ).coerceAtLeast(0)

                        binding.tvLoveDays.text = resources.getQuantityString(
                            R.plurals.home_love_days,
                            days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                            days
                        )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (supportFragmentManager.isStateSaved) return
        val requestedTab = intent.getStringExtra(EXTRA_TAB) ?: return
        intent.removeExtra(EXTRA_TAB)
        when (requestedTab) {
            TAB_HOME -> showHomeTab()
            TAB_MEMORY -> showMemoryTab()
        }
    }

    private fun showHomeTab() {
        val manager = supportFragmentManager
        if (!manager.isStateSaved && manager.findFragmentById(R.id.homeMemoryContainer) is MemoryListFragment) {
            manager.popBackStack("home_memory", androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun showMemoryTab() {
        val manager = supportFragmentManager
        val tab = binding.layoutBottomNav.navMemory
        if (!tab.isEnabled || manager.isStateSaved ||
            manager.findFragmentById(R.id.homeMemoryContainer) != null
        ) return

        tab.isEnabled = false
        manager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.homeMemoryContainer, MemoryListFragment())
            .addToBackStack("home_memory")
            .commit()
    }

    companion object {
        const val EXTRA_TAB = "com.example.lovetester.extra.HOME_TAB"
        const val TAB_HOME = "home"
        const val TAB_MEMORY = "memory"
    }

    private fun renderMemoryNavigation() {
        val current = supportFragmentManager.findFragmentById(R.id.homeMemoryContainer)
        val onMemory = current is MemoryListFragment
        val onAdd = current is AddMemoryFragment
        val showingMemory = current != null
        // Home artwork reaches the window bottom behind the transparent system navigation bar.
        // Show only the Home content or the Memory content while keeping the shared tab bar.
        binding.ivHomeBackground.isVisible = !showingMemory
        binding.ivHomeContentBackground.isVisible = !showingMemory
        binding.ivCouple.isVisible = !showingMemory
        binding.layoutCoupleInfo.isVisible = !showingMemory
        binding.btnSettings.isVisible = !showingMemory
        binding.btnGallery.isVisible = !showingMemory
        binding.homeMemoryContainer.isVisible = showingMemory
        binding.layoutBottomNav.root.isVisible = !onAdd
        binding.layoutBottomNav.root.setBackgroundColor(if (onMemory) Color.WHITE else Color.TRANSPARENT)
        binding.layoutBottomNav.navMemory.isEnabled = true
        binding.layoutBottomNav.navMemory.isSelected = onMemory
        binding.layoutBottomNav.navHome.isSelected = current == null

        val selected = getColor(R.color.primary)
        val unselected = Color.parseColor("#454954")
        val homeColor = if (onMemory) unselected else selected
        val memoryColor = if (onMemory) selected else unselected
        binding.layoutBottomNav.ivNavHome.setColorFilter(homeColor)
        binding.layoutBottomNav.tvNavHome.setTextColor(homeColor)
        binding.layoutBottomNav.ivNavMemory.setColorFilter(memoryColor)
        binding.layoutBottomNav.tvNavMemory.setTextColor(memoryColor)
        androidx.core.view.WindowCompat.getInsetsController(window, binding.root)
            .isAppearanceLightStatusBars = current != null
        ViewCompat.requestApplyInsets(binding.root)
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
            binding.layoutBottomNav.root.layoutParams as ViewGroup.MarginLayoutParams
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

            val onAdd = supportFragmentManager.findFragmentById(R.id.homeMemoryContainer) is AddMemoryFragment
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.homeMemoryContainer.setPadding(
                navigationInsets.left,
                statusInsets.top,
                navigationInsets.right,
                if (onAdd) maxOf(navigationInsets.bottom, ime.bottom) else 0
            )

            binding.layoutBottomNav.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
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