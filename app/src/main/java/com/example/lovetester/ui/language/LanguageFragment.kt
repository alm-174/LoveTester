package com.example.lovetester.ui.language

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLanguageBinding
import kotlinx.coroutines.launch

class LanguageFragment : Fragment(R.layout.fragment_language) {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: LanguageViewModel by viewModels()

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
        val adapter = LanguageAdapter(
            languages = SupportedLanguages.all,
            selectedId = viewModel.selectedId.value
        ) { language ->
            // Only highlight. The language is applied when the user taps the check icon.
            viewModel.select(language)
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLanguages.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedId.collect { adapter.selectedId = it }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnConfirmLanguage.setOnClickListener {
            val selectedLanguage = viewModel.selected

            // Leave this screen first, then apply: applying recreates SettingsActivity,
            // which should come back showing the settings list (not the language list).
            parentFragmentManager.popBackStack()
            SupportedLanguages.apply(selectedLanguage)
        }
    }

    override fun onDestroyView() {
        binding.rvLanguages.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
