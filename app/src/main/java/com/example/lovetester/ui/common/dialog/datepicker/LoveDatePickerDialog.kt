package com.example.lovetester.ui.common.dialog.datepicker

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lovetester.R
import com.example.lovetester.databinding.DialogLoveDatePickerBinding
import com.example.lovetester.ui.common.viewBinding
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
        private const val STATE_SELECTED_DATE = "selected_date_state"
        private const val STATE_DISPLAYED_MONTH = "displayed_month_state"
        private const val STATE_YEAR_PICKER_VISIBLE = "year_picker_visible_state"

        fun newInstance(
            initialDate: LocalDate = LocalDate.now(),
        ): LoveDatePickerDialog {
            return LoveDatePickerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_DATE, initialDate.toString())
                }
            }
        }
    }

    private val binding by viewBinding(DialogLoveDatePickerBinding::bind)

    private lateinit var dayAdapter: CalendarDayAdapter
    private lateinit var yearAdapter: CalendarYearAdapter

    private var maximumDate: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate = LocalDate.now()
    private var displayedMonth: YearMonth = YearMonth.now()
    private var isYearPickerVisible = false

    private val monthFormatter =
        DateTimeFormatter.ofPattern("MMMM - yyyy", Locale.getDefault())

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        maximumDate = LocalDate.now()

        val initialDate = arguments
            ?.getString(ARG_INITIAL_DATE)
            ?.let(LocalDate::parse)
            ?: maximumDate

        selectedDate = savedInstanceState
            ?.getString(STATE_SELECTED_DATE)
            ?.let(LocalDate::parse)
            ?.coerceAtMost(maximumDate)
            ?: initialDate.coerceAtMost(maximumDate)

        displayedMonth = savedInstanceState
            ?.getString(STATE_DISPLAYED_MONTH)
            ?.let(YearMonth::parse)
            ?.coerceAtMost(YearMonth.from(maximumDate))
            ?: YearMonth.from(selectedDate)

        isYearPickerVisible = savedInstanceState
            ?.getBoolean(STATE_YEAR_PICKER_VISIBLE)
            ?: false

        setupCalendar()
        setupListeners()
        renderCalendar()
    }

    private fun setupCalendar() {
        dayAdapter = CalendarDayAdapter { clickedDate ->
            if (!clickedDate.isAfter(maximumDate)) {
                selectedDate = clickedDate
                displayedMonth = YearMonth.from(clickedDate)
                renderCalendar()
            }
        }

        binding.rvCalendarDays.layoutManager =
            GridLayoutManager(requireContext(), 7)
        binding.rvCalendarDays.adapter = dayAdapter

        yearAdapter = CalendarYearAdapter { selectedYear ->
            val requestedMonth = YearMonth.of(
                selectedYear,
                displayedMonth.monthValue
            )

            displayedMonth = requestedMonth.coerceAtMost(
                YearMonth.from(maximumDate)
            )
            isYearPickerVisible = false
            renderCalendar()
        }

        binding.rvCalendarYears.layoutManager =
            GridLayoutManager(requireContext(), 4)
        binding.rvCalendarYears.adapter = yearAdapter
    }

    private fun setupListeners() {
        binding.btnPreviousMonth.setOnClickListener {
            if (isYearPickerVisible) {
                return@setOnClickListener
            }

            displayedMonth = displayedMonth.minusMonths(1)
            renderCalendar()
        }

        binding.btnNextMonth.setOnClickListener {
            if (
                isYearPickerVisible ||
                (displayedMonth >= YearMonth.from(maximumDate))
            ) {
                return@setOnClickListener
            }

            displayedMonth = displayedMonth.plusMonths(1)
            renderCalendar()
        }

        binding.tvCalendarMonth.setOnClickListener {
            isYearPickerVisible = !isYearPickerVisible
            renderCalendar()
        }

        binding.btnCancelDate.setOnClickListener {
            dismiss()
        }

        binding.btnSaveDate.setOnClickListener {
            val today = LocalDate.now()
            val resultDate = selectedDate.coerceAtMost(today)

            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(RESULT_DATE, resultDate.toString())
                }
            )

            dismiss()
        }
    }

    private fun renderCalendar() {
        val maximumMonth = YearMonth.from(maximumDate)
        displayedMonth = displayedMonth.coerceAtMost(maximumMonth)

        binding.tvCalendarMonth.text =
            displayedMonth.format(monthFormatter)

        binding.layoutWeekHeader.visibility =
            if (isYearPickerVisible) View.INVISIBLE else View.VISIBLE
        binding.rvCalendarDays.visibility =
            if (isYearPickerVisible) View.INVISIBLE else View.VISIBLE
        binding.rvCalendarYears.visibility =
            if (isYearPickerVisible) View.VISIBLE else View.GONE

        val canGoToPreviousMonth =
            !isYearPickerVisible && displayedMonth > YearMonth.of(1, 1)
        val canGoToNextMonth =
            !isYearPickerVisible && displayedMonth < maximumMonth

        binding.btnPreviousMonth.isEnabled = canGoToPreviousMonth
        binding.btnPreviousMonth.alpha =
            if (canGoToPreviousMonth) 1f else 0.35f

        binding.btnNextMonth.isEnabled = canGoToNextMonth
        binding.btnNextMonth.alpha =
            if (canGoToNextMonth) 1f else 0.35f

        if (isYearPickerVisible) {
            renderYears()
        } else {
            dayAdapter.submitDays(buildCalendarDays())
        }
    }

    private fun renderYears() {
        val years = (1..maximumDate.year).toList()

        yearAdapter.submitYears(
            newYears = years,
            selectedYear = displayedMonth.year,
            currentYear = maximumDate.year
        )

        binding.rvCalendarYears.post {
            val position = displayedMonth.year - 1
            binding.rvCalendarYears.scrollToPosition(position)
        }
    }

    private fun buildCalendarDays(): List<CalendarDay> {
        val firstDay = displayedMonth.atDay(1)

        // Java DayOfWeek: Monday = 1, Sunday = 7.
        // Chuyển sang Sunday = 0.
        val offset = firstDay.dayOfWeek.value % 7
        val gridStartDate =
            firstDay.minusDays(offset.toLong())

        val cells = offset + displayedMonth.lengthOfMonth()
        val totalCells = ((cells + 6) / 7) * 7

        return (0 until totalCells).map { index ->
            val date =
                gridStartDate.plusDays(index.toLong())

            CalendarDay(
                date = date,
                isCurrentMonth =
                    YearMonth.from(date) == displayedMonth,
                isSelected = date == selectedDate,
                isEnabled = !date.isAfter(maximumDate)
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            STATE_SELECTED_DATE,
            selectedDate.toString()
        )
        outState.putString(
            STATE_DISPLAYED_MONTH,
            displayedMonth.toString()
        )
        outState.putBoolean(
            STATE_YEAR_PICKER_VISIBLE,
            isYearPickerVisible
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
        binding.rvCalendarYears.adapter = null
        super.onDestroyView()
    }
}
