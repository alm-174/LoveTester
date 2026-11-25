package com.example.lovetester.ui.setting

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivitySettingsBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.language.LanguageFragment

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableLightEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        supportFragmentManager.addOnBackStackChangedListener {
            updateScreen(
                supportFragmentManager.backStackEntryCount > 0
            )
        }

        updateScreen(
            supportFragmentManager.backStackEntryCount > 0
        )
        setupListeners()


    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutLanguage.setOnClickListener {
            openLanguageFragment()
        }

    }

    private fun openLanguageFragment() {

        if (supportFragmentManager.backStackEntryCount > 0) {
            return
        }

        updateScreen(true)

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.settingsFragmentContainer,
                LanguageFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    private fun updateScreen(isLanguageOpen: Boolean) {

        binding.layoutSettingsHeader.visibility =
            if (isLanguageOpen) View.GONE else View.VISIBLE

        binding.layoutSettingsOptions.visibility =
            if (isLanguageOpen) View.GONE else View.VISIBLE

        binding.settingsFragmentContainer.visibility =
            if (isLanguageOpen) View.VISIBLE else View.GONE
    }
}