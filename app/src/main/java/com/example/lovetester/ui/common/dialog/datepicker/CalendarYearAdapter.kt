package com.example.lovetester.ui.common.dialog.datepicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemCalendarYearBinding
import java.util.Locale

class CalendarYearAdapter(
    private val onYearClick: (Int) -> Unit
) : ListAdapter<CalendarYearAdapter.YearItem, CalendarYearAdapter.YearViewHolder>(DIFF_CALLBACK) {

    /** The highlight state is part of the item, so DiffUtil redraws a year when it changes. */
    data class YearItem(
        val year: Int,
        val isSelected: Boolean,
        val isCurrent: Boolean
    )

    class YearViewHolder(
        val binding: ItemCalendarYearBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): YearViewHolder {
        val binding = ItemCalendarYearBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return YearViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: YearViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        val yearView = holder.binding.tvYear
        val context = yearView.context

        // "%d" never inserts a thousands separator ("2026", not "2,026").
        yearView.text = String.format(Locale.getDefault(), "%d", item.year)

        if (item.isSelected) {
            yearView.setBackgroundResource(R.drawable.bg_calendar_day_selected)
            yearView.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            yearView.background = null
            yearView.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.isCurrent) R.color.primary else R.color.text_title
                )
            )
        }

        holder.binding.root.setOnClickListener {
            onYearClick(item.year)
        }
    }

    fun submitYears(
        newYears: List<Int>,
        selectedYear: Int,
        currentYear: Int
    ) {
        submitList(
            newYears.map { year ->
                YearItem(year, year == selectedYear, year == currentYear)
            }
        )
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<YearItem>() {
            override fun areItemsTheSame(oldItem: YearItem, newItem: YearItem) =
                oldItem.year == newItem.year

            override fun areContentsTheSame(oldItem: YearItem, newItem: YearItem) =
                oldItem == newItem
        }
    }
}
