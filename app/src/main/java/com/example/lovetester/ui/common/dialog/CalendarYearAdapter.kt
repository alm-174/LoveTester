package com.example.lovetester.ui.common.dialog

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemCalendarYearBinding

class CalendarYearAdapter(
    private val onYearClick: (Int) -> Unit
) : RecyclerView.Adapter<CalendarYearAdapter.YearViewHolder>() {

    private var years: List<Int> = emptyList()
    private var selectedYear: Int = 0
    private var currentYear: Int = 0

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
        val year = years[position]
        val isSelected = year == selectedYear

        holder.binding.tvYear.text = year.toString()

        if (isSelected) {
            holder.binding.tvYear.setBackgroundResource(
                R.drawable.bg_calendar_day_selected
            )
            holder.binding.tvYear.setTextColor(Color.WHITE)
        } else {
            holder.binding.tvYear.background = null
            holder.binding.tvYear.setTextColor(
                if (year == currentYear) {
                    Color.parseColor("#FF0845")
                } else {
                    Color.parseColor("#1E232E")
                }
            )
        }

        holder.binding.root.setOnClickListener {
            onYearClick(year)
        }
    }

    override fun getItemCount(): Int {
        return years.size
    }

    fun submitYears(
        newYears: List<Int>,
        selectedYear: Int,
        currentYear: Int
    ) {
        years = newYears
        this.selectedYear = selectedYear
        this.currentYear = currentYear
        notifyDataSetChanged()
    }
}
