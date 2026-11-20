package com.example.lovetester.ui.lovetest

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityLoveTestBinding
import com.example.lovetester.ui.home.HomeActivity

class LoveTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoveTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = lightBars)
        binding = ActivityLoveTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tabs = binding.layoutBottomNav
        val active = getColor(R.color.color_ff0845)
        val inactive = Color.parseColor("#454954")
        tabs.ivNavHome.setColorFilter(inactive)
        tabs.tvNavHome.setTextColor(inactive)
        tabs.ivNavLoveTest.setColorFilter(active)
        tabs.tvNavLoveTest.setTextColor(active)
        tabs.navLoveTest.isSelected = true
        tabs.root.setBackgroundColor(Color.WHITE)
        tabs.navHome.setOnClickListener { returnToHomeTab(HomeActivity.TAB_HOME) }
        tabs.navMemory.setOnClickListener { returnToHomeTab(HomeActivity.TAB_MEMORY) }

        applyWindowInsets()
        val host = supportFragmentManager.findFragmentById(R.id.loveTestNavHost) as NavHostFragment
        host.navController.addOnDestinationChangedListener { _, destination, _ ->
            tabs.root.isVisible = destination.id == R.id.loveTestFragment
            ViewCompat.requestApplyInsets(binding.root)
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun returnToHomeTab(tab: String) {
        if (isFinishing) return
        startActivity(Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(HomeActivity.EXTRA_TAB, tab)
        })
        finish()
    }

    private fun applyWindowInsets() {
        val tabs = binding.layoutBottomNav.root
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.root.setPadding(safe.left, safe.top, safe.right, 0)
            tabs.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = safe.bottom
            }
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.loveTestNavHost.setPadding(
                0, 0, 0, if (tabs.isVisible) 0 else maxOf(safe.bottom, ime.bottom)
            )
            insets
        }
    }
}
