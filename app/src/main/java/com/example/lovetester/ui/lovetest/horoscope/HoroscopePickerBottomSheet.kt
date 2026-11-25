package com.example.lovetester.ui.lovetest.horoscope


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.lovetester.R
import com.example.lovetester.databinding.BottomSheetHoroscopeBinding
import com.example.lovetester.ui.background.BackgroundGridSpacing
import com.example.lovetester.ui.common.expandWithMaxHeight

class HoroscopePickerBottomSheet :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetHoroscopeBinding? = null

    private val binding: BottomSheetHoroscopeBinding
        get() = requireNotNull(_binding)

    private val requestKey: String
        get() = requireArguments().getString(ARG_REQUEST_KEY)!!

    private val selectedSign: ZodiacSign?
        get() = requireArguments().getString(ARG_SELECTED)?.let { ZodiacSign.valueOf(it) }

    companion object {
        const val TAG = "HoroscopePickerBottomSheet"
        const val RESULT_SIGN = "result_sign"

        private const val ARG_REQUEST_KEY = "arg_request_key"
        private const val ARG_SELECTED = "arg_selected"

        fun newInstance(requestKey: String, selected: ZodiacSign?) =
            HoroscopePickerBottomSheet().apply {
                arguments = bundleOf(
                    ARG_REQUEST_KEY to requestKey,
                    ARG_SELECTED to selected?.name
                )
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHoroscopeBinding.inflate(
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

        setupRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        expandWithMaxHeight(R.dimen.sheet_max_height_horoscope)
    }

    private fun setupRecyclerView() {
        val spacing = (16 * resources.displayMetrics.density + 0.5f).toInt()
        binding.rvZodiac.layoutManager = GridLayoutManager(requireContext(), 2)
        if (binding.rvZodiac.itemDecorationCount == 0) {
            binding.rvZodiac.addItemDecoration(BackgroundGridSpacing(spacing))
        }

        binding.rvZodiac.adapter = ZodiacAdapter(selectedSign) { sign ->
            setFragmentResult(requestKey, bundleOf(RESULT_SIGN to sign.name))
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding.rvZodiac.adapter = null
        _binding = null
        super.onDestroyView()
    }
}