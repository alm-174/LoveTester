package com.example.lovetester.ui.common.dialog.datepicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemCalendarDayBinding
import java.time.LocalDate
import java.util.Locale

class CalendarDayAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : ListAdapter<CalendarDay, CalendarDayAdapter.DayViewHolder>(DIFF_CALLBACK) {

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
        val day = getItem(position)
        val dayTextView = holder.binding.tvDayNumber
        val context = dayTextView.context

        dayTextView.text = String.format(Locale.getDefault(), "%d", day.date.dayOfMonth)
        dayTextView.alpha = if (day.isEnabled) 1f else 0.35f
        holder.binding.root.isEnabled = day.isEnabled

        if (day.isSelected && day.isEnabled) {
            dayTextView.setBackgroundResource(
                R.drawable.bg_calendar_day_selected
            )
            dayTextView.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            dayTextView.background = null

            val textColor = if (day.isEnabled && day.isCurrentMonth) {
                R.color.text_title
            } else {
                R.color.text_disabled
            }
            dayTextView.setTextColor(ContextCompat.getColor(context, textColor))
        }

        holder.binding.root.setOnClickListener {
            if (day.isEnabled) {
                onDayClick(day.date)
            }
        }
    }

    fun submitDays(newDays: List<CalendarDay>) {
        submitList(newDays)
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CalendarDay>() {
            override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay) =
                oldItem.date == newItem.date

            override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay) =
                oldItem == newItem
        }
    }
}
