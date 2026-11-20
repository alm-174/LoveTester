package com.example.lovetester.ui.lovetest.horoscope

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentHoroscopeTestBinding

class HoroscopeTestFragment :
    Fragment(R.layout.fragment_horoscope_test) {

    private var _binding: FragmentHoroscopeTestBinding? = null

    private val binding: FragmentHoroscopeTestBinding
        get() = requireNotNull(_binding)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHoroscopeTestBinding.bind(view)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}