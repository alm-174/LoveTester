package com.example.lovetester.ui.startdate

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.R
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.FragmentStartDateBinding
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StartDateFragment : Fragment(R.layout.fragment_start_date) {

    private var _binding: FragmentStartDateBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var repository: LoveSettingsRepository

    private var selectedDate: LocalDate? = null

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStartDateBinding.bind(view)

        applyWindowInsets()
        setStatusBarDarkIcons(enabled = true)

        repository = LoveSettingsRepository(
            requireContext().applicationContext
        )

        // Nhận kết quả từ Dialog
        childFragmentManager.setFragmentResultListener(
            LoveDatePickerDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            val dateString = bundle.getString(
                LoveDatePickerDialog.RESULT_DATE
            )

            selectedDate = dateString
                ?.let(LocalDate::parse)

            updateSelectedDate()
        }

        // Giữ ngày đang chỉnh sửa khi Fragment được tái tạo
        val restoredDate = savedInstanceState
            ?.getString("pending_date")
            ?.let(LocalDate::parse)

        if (restoredDate != null) {
            selectedDate = restoredDate
            updateSelectedDate()
        } else {
            loadSavedDate()
        }

        setupListeners()
    }

    private fun loadSavedDate() {
        viewLifecycleOwner.lifecycleScope.launch {
            selectedDate = repository.loveStartDate.first()

            updateSelectedDate()
        }
    }

    private fun setupListeners() {

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.layoutDateOfLove.setOnClickListener {
            showDatePicker()
        }

        binding.btnDone.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun showDatePicker() {

        val dialog = LoveDatePickerDialog.newInstance(
            selectedDate ?: LocalDate.now()
        )

        childFragmentManager.showOnce(dialog, "LoveDatePickerDialog")
    }

    private fun updateSelectedDate() {
        binding.tvSelectedDate.text =
            selectedDate?.format(dateFormatter)
                ?: getString(R.string.common_select_date)
    }

    private fun saveAndFinish() {

        val date = selectedDate ?: return

        binding.btnDone.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.setLoveStartDate(date)

                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                android.widget.Toast.makeText(
                    requireContext(),
                    getString(R.string.start_date_save_error),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun applyWindowInsets() {
        val initialRootLeft = binding.root.paddingLeft
        val initialRootRight = binding.root.paddingRight

        val headerParams =
            binding.layoutStartDateHeader.layoutParams as ViewGroup.MarginLayoutParams
        val initialHeaderTop = headerParams.topMargin

        val doneParams =
            binding.btnDone.layoutParams as ViewGroup.MarginLayoutParams
        val initialDoneBottom = doneParams.bottomMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val statusInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            val navigationInsets = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )

            view.setPadding(
                initialRootLeft + systemInsets.left,
                view.paddingTop,
                initialRootRight + systemInsets.right,
                view.paddingBottom
            )

            binding.layoutStartDateHeader
                .updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = initialHeaderTop + statusInsets.top
                }

            binding.btnDone.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialDoneBottom + navigationInsets.bottom
            }

            insets
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun setStatusBarDarkIcons(enabled: Boolean) {
        val window = activity?.window ?: return
        WindowCompat.getInsetsController(
            window,
            binding.root
        ).isAppearanceLightStatusBars = enabled
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "pending_date",
            selectedDate?.toString()
        )
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        setStatusBarDarkIcons(enabled = false)
        _binding = null
        super.onDestroyView()
    }
}
