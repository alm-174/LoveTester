package com.example.lovetester.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityLanguageBinding
import com.example.lovetester.ui.intro.IntroActivity
class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            val selectedLanguage = adapter.getSelectedLanguage()

            // TODO: Sau khi tạo IntroActivity,
            // sử dụng Intent để mở màn Intro.

            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }
    }
}