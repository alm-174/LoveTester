package com.example.lovetester.ui.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivitySettingsBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.language.LanguageActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableLightEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutLanguage.setOnClickListener {
            if (isNavigating) return@setOnClickListener
            isNavigating = true
            startActivity(LanguageActivity.newIntent(this, fromSettings = true))
        }
    }
}