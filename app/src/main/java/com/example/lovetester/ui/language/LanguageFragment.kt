package com.example.lovetester.ui.language

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLanguageBinding

class LanguageFragment : Fragment(R.layout.fragment_language){
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLanguageBinding.bind(view)

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
            // Hiện tại chỉ chọn ngôn ngữ.
            // Chưa thay đổi Locale của ứng dụng.
        }

        binding.rvLanguages.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvLanguages.adapter = adapter

        binding.btnConfirmLanguage.setOnClickListener {
            val selectedLanguage = adapter.getSelectedLanguage()

            // Bước tiếp theo:
            // Chuyển sang màn Intro đầu tiên.
            // Chưa cần lưu selectedLanguage.
        }
    }
}