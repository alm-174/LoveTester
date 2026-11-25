package com.example.lovetester.ui.lovetest.horoscope

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentHoroscopeTestBinding
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.safeNavigate
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.common.viewBinding
import com.example.lovetester.ui.lovetest.result.LoveTestResultFragment

class HoroscopeTestFragment :
    Fragment(R.layout.fragment_horoscope_test) {

    private val binding by viewBinding(FragmentHoroscopeTestBinding::bind)

    private val viewModel: HoroscopeTestViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupResultListeners()
        setupClicks()
        observeState()
    }

    private fun setupResultListeners() {
        childFragmentManager.setFragmentResultListener(
            REQUEST_MY_SIGN, viewLifecycleOwner
        ) { _, bundle ->
            bundle.toZodiacSign()?.let { viewModel.setMySign(it) }
        }

        childFragmentManager.setFragmentResultListener(
            REQUEST_PARTNER_SIGN, viewLifecycleOwner
        ) { _, bundle ->
            bundle.toZodiacSign()?.let { viewModel.setPartnerSign(it) }
        }
    }

    private fun Bundle.toZodiacSign(): ZodiacSign? =
        getString(HoroscopePickerBottomSheet.RESULT_SIGN)?.let { ZodiacSign.valueOf(it) }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.layoutYourZodiac.setOnClickListener {
            openPicker(REQUEST_MY_SIGN, viewModel.uiState.value.mySign)
        }
        binding.layoutPartnerZodiac.setOnClickListener {
            openPicker(REQUEST_PARTNER_SIGN, viewModel.uiState.value.partnerSign)
        }

        binding.btnLoveTest.setOnClickListener {
            val state = viewModel.uiState.value
            val percentage = viewModel.calculatePercent()

            val args = bundleOf(
                LoveTestResultFragment.ARG_PERSON_ONE to getString(state.mySign.nameRes),
                LoveTestResultFragment.ARG_PERSON_TWO to getString(state.partnerSign.nameRes),
                LoveTestResultFragment.ARG_PERCENTAGE to percentage,
                LoveTestResultFragment.ARG_TEST_TYPE to LoveTestResultFragment.TEST_TYPE_HOROSCOPE,
                LoveTestResultFragment.ARG_SIGN_ONE to state.mySign.name,
                LoveTestResultFragment.ARG_SIGN_TWO to state.partnerSign.name
            )

            findNavController().safeNavigate(R.id.action_horoscopeTest_to_result, args)
        }
    }

    private fun openPicker(requestKey: String, current: ZodiacSign) {
        childFragmentManager.showOnce(
            HoroscopePickerBottomSheet.newInstance(requestKey, current),
            HoroscopePickerBottomSheet.TAG
        )
    }

    private fun observeState() {
        viewModel.uiState.collectOn(viewLifecycleOwner) { state ->
            binding.ivYourZodiac.setImageResource(state.mySign.iconRes)
            binding.tvYourZodiac.setText(state.mySign.nameRes)

            binding.ivPartnerZodiac.setImageResource(state.partnerSign.iconRes)
            binding.tvPartnerZodiac.setText(state.partnerSign.nameRes)
        }
    }

    companion object {
        private const val REQUEST_MY_SIGN = "request_my_sign"
        private const val REQUEST_PARTNER_SIGN = "request_partner_sign"
    }

}