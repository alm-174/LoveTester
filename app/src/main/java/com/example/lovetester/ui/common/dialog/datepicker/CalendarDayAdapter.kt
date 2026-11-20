package com.example.lovetester.ui.common.dialog.datepicker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemCalendarDayBinding
import java.time.LocalDate

class CalendarDayAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    private var days: List<CalendarDay> = emptyList()

    class DayViewHolder(
        val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DayViewHolder,
        position: Int
    ) {
        val day = days[position]
        val dayTextView = holder.binding.tvDayNumber

        dayTextView.text = day.date.dayOfMonth.toString()
        dayTextView.alpha = if (day.isEnabled) 1f else 0.35f
        holder.binding.root.isEnabled = day.isEnabled

        if (day.isSelected && day.isEnabled) {
            dayTextView.setBackgroundResource(
                R.drawable.bg_calendar_day_selected
            )
            dayTextView.setTextColor(Color.WHITE)
        } else {
            dayTextView.background = null

            val textColor = when {
                !day.isEnabled -> Color.parseColor("#B8BBC4")
                day.isCurrentMonth -> Color.parseColor("#1E232E")
                else -> Color.parseColor("#B8BBC4")
            }
            dayTextView.setTextColor(textColor)
        }

        holder.binding.root.setOnClickListener {
            if (day.isEnabled) {
                onDayClick(day.date)
            }
        }
    }

    override fun getItemCount(): Int {
        return days.size
    }

    fun submitDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}
