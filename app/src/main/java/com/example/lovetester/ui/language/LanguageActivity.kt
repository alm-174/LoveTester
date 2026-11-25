package com.example.lovetester.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivityLanguageBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.intro.IntroActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: LoveSettingsRepository

    private lateinit var binding: ActivityLanguageBinding

    private val viewModel: LanguageViewModel by viewModels()

    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableLightEdgeToEdge()
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        setupLanguageList()
        setupConfirmButton()
    }

    private fun setupLanguageList() {
        val adapter = LanguageAdapter(
            languages = SupportedLanguages.all,
            selectedId = viewModel.selectedId.value
        ) { language ->
            // Only highlight. The language is applied when the user taps the check icon.
            viewModel.select(language)
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter

        viewModel.selectedId.collectOn(this) { adapter.selectedId = it }
    }

    private fun setupConfirmButton() {
        binding.btnConfirmLanguage.setOnClickListener {
            if (isNavigating) return@setOnClickListener

            val selectedLanguage = viewModel.selected

            isNavigating = true
            binding.btnConfirmLanguage.isEnabled = false

            lifecycleScope.launch {
                try {
                    repository.setOnboardingStep(OnboardingStep.INTRO)

                    // Apply right before leaving. Both calls run back-to-back on the main
                    // thread, so IntroActivity is created already using the new language.
                    SupportedLanguages.apply(selectedLanguage)

                    startActivity(Intent(this@LanguageActivity, IntroActivity::class.java))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    isNavigating = false
                    binding.btnConfirmLanguage.isEnabled = true
                    // TODO: Hiển thị thông báo không thể lưu trạng thái
                }
            }
        }
    }
}
