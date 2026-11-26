package com.example.lovetester.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityHomeBinding
import com.example.lovetester.ui.lovetest.LoveTestFragment
import com.example.lovetester.ui.memory.MemoryListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private enum class Tab(val tag: String) {
        HOME("tab_home"),
        LOVE_TEST("tab_love_test"),
        MEMORY("tab_memory");

        fun createFragment(): Fragment = when (this) {
            HOME -> HomeFragment()
            LOVE_TEST -> LoveTestFragment()
            MEMORY -> MemoryListFragment()
        }
    }

    private lateinit var binding: ActivityHomeBinding
    private var currentTab = Tab.HOME

    private val backToHomeCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = selectTab(Tab.HOME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyBottomNavInsets()

        onBackPressedDispatcher.addCallback(this, backToHomeCallback)

        binding.layoutBottomNav.navHome.setOnClickListener { selectTab(Tab.HOME) }
        binding.layoutBottomNav.navLoveTest.setOnClickListener { selectTab(Tab.LOVE_TEST) }
        binding.layoutBottomNav.navMemory.setOnClickListener { selectTab(Tab.MEMORY) }

        currentTab = savedInstanceState?.getString(KEY_CURRENT_TAB)?.let(Tab::valueOf)
            ?: tabFromIntent(intent)
                    ?: Tab.HOME

        selectTab(currentTab)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tabFromIntent(intent)?.let(::selectTab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CURRENT_TAB, currentTab.name)
    }

    private fun tabFromIntent(intent: Intent): Tab? = when (intent.getStringExtra(EXTRA_TAB)) {
        TAB_HOME -> Tab.HOME
        TAB_LOVE_TEST -> Tab.LOVE_TEST
        TAB_MEMORY -> Tab.MEMORY
        else -> null
    }

    private fun selectTab(tab: Tab) {
        val fm = supportFragmentManager
        if (fm.isStateSaved) return

        val tx = fm.beginTransaction().setReorderingAllowed(true)
        Tab.entries.forEach { t ->
            val fragment = fm.findFragmentByTag(t.tag)
            when {
                t == tab && fragment == null -> tx.add(R.id.homeTabContainer, t.createFragment(), t.tag)
                t == tab -> tx.show(fragment!!)
                fragment != null -> tx.hide(fragment)
            }
        }
        tx.commit()

        currentTab = tab
        backToHomeCallback.isEnabled = tab != Tab.HOME
        renderTabs()
    }

    private fun renderTabs() {
        val selected = getColor(R.color.primary)
        val unselected = getColor(R.color.tab_inactive)

        val homeColor = if (currentTab == Tab.HOME) selected else unselected
        val loveTestColor = if (currentTab == Tab.LOVE_TEST) selected else unselected
        val memoryColor = if (currentTab == Tab.MEMORY) selected else unselected

        binding.layoutBottomNav.navHome.isSelected = currentTab == Tab.HOME
        binding.layoutBottomNav.ivNavHome.setColorFilter(homeColor)
        binding.layoutBottomNav.tvNavHome.setTextColor(homeColor)

        binding.layoutBottomNav.navLoveTest.isSelected = currentTab == Tab.LOVE_TEST
        binding.layoutBottomNav.ivNavLoveTest.setColorFilter(loveTestColor)
        binding.layoutBottomNav.tvNavLoveTest.setTextColor(loveTestColor)

        binding.layoutBottomNav.navMemory.isSelected = currentTab == Tab.MEMORY
        binding.layoutBottomNav.ivNavMemory.setColorFilter(memoryColor)
        binding.layoutBottomNav.tvNavMemory.setTextColor(memoryColor)

        binding.layoutBottomNav.root.setBackgroundColor(
            if (currentTab == Tab.HOME) Color.TRANSPARENT else Color.WHITE
        )

        WindowCompat.getInsetsController(window, binding.root)
            .isAppearanceLightStatusBars = currentTab != Tab.HOME
    }

    private fun applyBottomNavInsets() {
        val bottomNavParams = binding.layoutBottomNav.root.layoutParams as ViewGroup.MarginLayoutParams
        val initialBottom = bottomNavParams.bottomMargin
        val initialStart = bottomNavParams.marginStart
        val initialEnd = bottomNavParams.marginEnd

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomNav.root) { view, insets ->
            val navInsets = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottom + navInsets.bottom
                marginStart = initialStart + navInsets.left
                marginEnd = initialEnd + navInsets.right
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.layoutBottomNav.root)
    }

    companion object {
        const val EXTRA_TAB = "com.example.lovetester.extra.HOME_TAB"
        const val TAB_HOME = "home"
        const val TAB_LOVE_TEST = "love_test"
        const val TAB_MEMORY = "memory"
        private const val KEY_CURRENT_TAB = "current_tab"
    }
}