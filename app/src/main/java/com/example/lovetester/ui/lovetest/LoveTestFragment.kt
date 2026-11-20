package com.example.lovetester.ui.lovetest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLoveTestBinding

class LoveTestFragment : Fragment() {

    private var _binding: FragmentLoveTestBinding? = null

    private val binding: FragmentLoveTestBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoveTestBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupTitle()
        setupCardClicks()
    }

    private fun setupTitle() {
        val text = "Love Test"

        val spannable = android.text.SpannableString(text)

        spannable.setSpan(
            android.text.style.ForegroundColorSpan(
                android.graphics.Color.parseColor("#FF0845")
            ),
            5,
            text.length,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvLoveTestTitle.text = spannable
    }

    private fun setupCardClicks() {

        binding.cardNameTest.setOnClickListener {
            // Điều hướng tới NameTestFragment
            findNavController().navigate(
                R.id.action_loveTest_to_nameTest
            )
        }

        binding.cardHoroscopeTest.setOnClickListener {
            // Điều hướng tới HoroscopeTestFragment
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}