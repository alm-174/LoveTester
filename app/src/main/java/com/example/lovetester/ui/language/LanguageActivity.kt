package com.example.lovetester.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityLanguageBinding
import com.example.lovetester.ui.intro.IntroActivity
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import kotlinx.coroutines.launch
class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding

    private lateinit var repository: LoveSettingsRepository
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LoveSettingsRepository(applicationContext)

        val languages = listOf(
            Language("en", R.string.language_english, R.drawable.flag_english),
            Language("hi", R.string.language_hindi, R.drawable.flag_hindi),
            Language("es", R.string.language_spanish, R.drawable.flag_spanish),
            Language("fr", R.string.language_french, R.drawable.flag_french),
            Language("pt", R.string.language_portuguese, R.drawable.flag_portuguese),
            Language("id", R.string.language_indonesian, R.drawable.flag_indonesian),
            Language("de", R.string.language_german, R.drawable.flag_german)
        )

        val adapter = LanguageAdapter(languages) { language ->
            // Hiện tại chưa thay đổi Locale
        }

        binding.rvLanguages.layoutManager =
            LinearLayoutManager(this)

        binding.rvLanguages.layoutManager =
            LinearLayoutManager(this)

        binding.rvLanguages.adapter = adapter




        binding.btnConfirmLanguage.setOnClickListener {
            if (isNavigating) return@setOnClickListener

            val selectedLanguage = adapter.getSelectedLanguage()

            isNavigating = true
            binding.btnConfirmLanguage.isEnabled = false

            lifecycleScope.launch {
                try {
                    repository.setOnboardingStep(OnboardingStep.INTRO)

                    val intent = Intent(
                        this@LanguageActivity,
                        IntroActivity::class.java
                    )
                    startActivity(intent)

                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e

                    isNavigating = false
                    binding.btnConfirmLanguage.isEnabled = true
                    // TODO: Hiển thị thông báo không thể lưu trạng thái
                }
            }
        }
    }
}