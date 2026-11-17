package com.example.lovetester.ui.common.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.DialogLoveDatePickerBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class LoveDatePickerDialog :
    DialogFragment(R.layout.dialog_love_date_picker) {

    companion object {
        const val REQUEST_KEY = "love_date_picker_result"
        const val RESULT_DATE = "selected_date"

        private const val ARG_INITIAL_DATE = "initial_date"

        fun newInstance(
            initialDate: LocalDate = LocalDate.now()
        ): LoveDatePickerDialog {
            return LoveDatePickerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_DATE, initialDate.toString())
                }
            }
        }
    }

    private var _binding: DialogLoveDatePickerBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var adapter: CalendarDayAdapter

    private var selectedDate: LocalDate = LocalDate.now()
    private var displayedMonth: YearMonth = YearMonth.now()

    private val monthFormatter =
        DateTimeFormatter.ofPattern("MMMM - yyyy", Locale.ENGLISH)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = DialogLoveDatePickerBinding.bind(view)

        val initialDate = arguments
            ?.getString(ARG_INITIAL_DATE)
            ?.let(LocalDate::parse)
            ?: LocalDate.now()

        selectedDate = savedInstanceState
            ?.getString("selected_date_state")
            ?.let(LocalDate::parse)
            ?: initialDate

        displayedMonth = savedInstanceState
            ?.getString("displayed_month_state")
            ?.let(YearMonth::parse)
            ?: YearMonth.from(selectedDate)

        setupCalendar()
        setupListeners()
        renderCalendar()
    }

    private fun setupCalendar() {
        adapter = CalendarDayAdapter { clickedDate ->
            selectedDate = clickedDate
            displayedMonth = YearMonth.from(clickedDate)
            renderCalendar()
        }

        binding.rvCalendarDays.layoutManager =
            GridLayoutManager(requireContext(), 7)

        binding.rvCalendarDays.adapter = adapter
    }

    private fun setupListeners() {

        binding.btnPreviousMonth.setOnClickListener {
            displayedMonth = displayedMonth.minusMonths(1)
            renderCalendar()
        }

        binding.btnNextMonth.setOnClickListener {
            displayedMonth = displayedMonth.plusMonths(1)
            renderCalendar()
        }

        binding.btnCancelDate.setOnClickListener {
            dismiss()
        }

        binding.btnSaveDate.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(RESULT_DATE, selectedDate.toString())
                }
            )

            dismiss()
        }
    }

    private fun renderCalendar() {
        binding.tvCalendarMonth.text =
            displayedMonth.format(monthFormatter)

        adapter.submitDays(buildCalendarDays())
    }

    private fun buildCalendarDays(): List<CalendarDay> {

        val firstDay = displayedMonth.atDay(1)

        // Java DayOfWeek: Monday = 1, Sunday = 7
        // Chuyển sang Sunday = 0
        val offset = firstDay.dayOfWeek.value % 7

        val gridStartDate =
            firstDay.minusDays(offset.toLong())

        val cells = offset + displayedMonth.lengthOfMonth()

        // Tạo đủ hàng, mỗi hàng có 7 ngày
        val totalCells = ((cells + 6) / 7) * 7

        return (0 until totalCells).map { index ->

            val date =
                gridStartDate.plusDays(index.toLong())

            CalendarDay(
                date = date,
                isCurrentMonth =
                    YearMonth.from(date) == displayedMonth,
                isSelected = date == selectedDate
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "selected_date_state",
            selectedDate.toString()
        )
        outState.putString(
            "displayed_month_state",
            displayedMonth.toString()
        )
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        binding.rvCalendarDays.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
