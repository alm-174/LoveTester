package com.example.lovetester.ui.language

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.R
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.FragmentLanguageBinding

class LanguageFragment: Fragment(R.layout.fragment_language) {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var repository: LoveSettingsRepository

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLanguageBinding.bind(view)

        setupLanguages()

        setupListeners()

    }

    private fun setupLanguages() {

        val languages = SupportedLanguages.all

        val adapter = LanguageAdapter(languages) { language ->
            // Adapter tự xử lý trạng thái chọn.
            // Chưa lưu hoặc áp dụng ngôn ngữ.
        }

        binding.rvLanguages.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvLanguages.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnConfirmLanguage.setOnClickListener {
            // TODO: Lưu và áp dụng ngôn ngữ sau khi hoàn thiện ứng dụng.
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}