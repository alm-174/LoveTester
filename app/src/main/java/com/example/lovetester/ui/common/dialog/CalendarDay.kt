package com.example.lovetester.ui.common.dialog

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isSelected: Boolean,
    val isEnabled: Boolean
)