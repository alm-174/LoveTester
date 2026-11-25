package com.example.lovetester.ui.home

import android.content.Intent
import android.graphics.Color
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
import coil3.load
import coil3.request.error
import com.example.lovetester.R
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.setScreenAnimations
import com.example.lovetester.ui.common.showOnce
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
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: LoveSettingsRepository

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
            supportFragmentManager.showOnce(BackgroundBottomSheet(), SHEET_TAG)
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

        repository.loveStartDate.collectOn(this) { startDate ->
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

        repository.selectedBackgroundId.collectOn(this) { id ->
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

        repository.youInfo.collectOn(this) { info ->
            binding.tvPersonOneName.text = info.name
            binding.ivPersonOneGender.setImageResource(genderIcon(info.gender))
            binding.tvPersonOneAge.text = calculateAge(info.birthday)
            renderAvatar(binding.ivPersonOne, info.avatarPath, R.drawable.avatar_sasuke)
        }

        repository.partnerInfo.collectOn(this) { info ->
            binding.tvPersonTwoName.text = info.name
            binding.ivPersonTwoGender.setImageResource(genderIcon(info.gender))
            binding.tvPersonTwoAge.text = calculateAge(info.birthday)
            renderAvatar(binding.ivPersonTwo, info.avatarPath, R.drawable.avatar_naruto)
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
            .setScreenAnimations()
            .replace(R.id.homeMemoryContainer, MemoryListFragment())
            .addToBackStack("home_memory")
            .commit()
    }

    companion object {
        const val EXTRA_TAB = "com.example.lovetester.extra.HOME_TAB"
        const val TAB_HOME = "home"
        const val TAB_MEMORY = "memory"

        // One tag for every bottom sheet opened from Home: only one can be open at a time.
        private const val SHEET_TAG = "home_sheet"
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
        // Hide the start-date container again once its fragment has been popped.
        binding.startDateContainer.isVisible =
            supportFragmentManager.findFragmentById(R.id.startDateContainer) != null
        binding.layoutBottomNav.root.isVisible = !onAdd
        binding.layoutBottomNav.root.setBackgroundColor(if (onMemory) Color.WHITE else Color.TRANSPARENT)
        binding.layoutBottomNav.navMemory.isEnabled = true
        binding.layoutBottomNav.navMemory.isSelected = onMemory
        binding.layoutBottomNav.navHome.isSelected = current == null

        val selected = getColor(R.color.primary)
        val unselected = getColor(R.color.tab_inactive)
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
        val manager = supportFragmentManager
        // Already open (or state saved): a second tap must not stack another copy.
        if (manager.isStateSaved || manager.findFragmentById(R.id.startDateContainer) != null) return

        binding.startDateContainer.visibility = View.VISIBLE

        manager.beginTransaction()
            .setScreenAnimations()
            .replace(
                R.id.startDateContainer,
                StartDateFragment()
            )
            .addToBackStack("start_date")
            .commit()
        // Register the fragment now so a fast double tap sees it.
        manager.executePendingTransactions()
    }

    private fun genderIcon(gender: Gender): Int {
        return when (gender) {
            Gender.MALE -> R.drawable.male
            Gender.FEMALE -> R.drawable.female
            Gender.OTHER -> R.drawable.other_gender
        }
    }

    private fun openInformation(personType: PersonType) {
        supportFragmentManager.showOnce(InformationBottomSheet.newInstance(personType), SHEET_TAG)
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

        // Coil decodes off the main thread, downsamples to the view size and cancels stale requests.
        if (file != null && file.isFile) {
            imageView.load(file) { error(defaultDrawable) }
        } else {
            imageView.load(defaultDrawable)
        }
    }
}