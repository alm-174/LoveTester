package com.example.lovetester.ui.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // LanguageFragment sẽ được kết nối ở bước tiếp theo.
        // Chưa gắn listener cho các mục chưa triển khai.
    }
}