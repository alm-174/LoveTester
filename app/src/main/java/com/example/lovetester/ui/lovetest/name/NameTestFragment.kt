package com.example.lovetester.ui.lovetest.name

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentNameTestBinding
import com.example.lovetester.ui.common.safeNavigate
import com.example.lovetester.ui.lovetest.domain.LoveTestCalculator

class NameTestFragment : Fragment(R.layout.fragment_name_test) {

    private var _binding: FragmentNameTestBinding? = null

    private val binding: FragmentNameTestBinding
        get() = requireNotNull(_binding)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNameTestBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnLoveTest.setOnClickListener {
            val yourName = binding.etYourName.text.toString().trim()
            val partnerName = binding.etPartnerName.text.toString().trim()

            if (yourName.isBlank()) {
                binding.etYourName.error = getString(R.string.name_test_error_empty_name)
                return@setOnClickListener
            }

            if (partnerName.isBlank()) {
                binding.etPartnerName.error = getString(R.string.name_test_error_empty_partner_name)
                return@setOnClickListener
            }

            val percentage = LoveTestCalculator.calculateNameTest(
                yourName,
                partnerName
            )

            val args = Bundle().apply {
                putString("personOne", yourName)
                putString("personTwo", partnerName)
                putInt("percentage", percentage)
            }

            findNavController().safeNavigate(
                R.id.action_nameTest_to_result,
                args
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}