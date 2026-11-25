package com.example.lovetester.ui.lovetest.horoscope

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemZodiacBinding

class ZodiacAdapter(
    private val selectedSign: ZodiacSign?,
    private val onSignClick: (ZodiacSign) -> Unit
) : RecyclerView.Adapter<ZodiacAdapter.ZodiacViewHolder>() {

    private val signs = ZodiacSign.entries

    inner class ZodiacViewHolder(
        private val binding: ItemZodiacBinding
    ): RecyclerView.ViewHolder(binding.root) {
        fun bind(sign: ZodiacSign) {
            binding.ivZodiac.setImageResource(sign.iconRes)
            binding.ivZodiac.contentDescription = binding.root.context.getString(sign.nameRes)

            binding.root.isSelected = sign == selectedSign

            binding.root.setOnClickListener {
                onSignClick(sign)
            }

            binding.root.setBackgroundResource(
                if (sign == selectedSign) R.drawable.bg_zodiac_item_selected
                else R.drawable.bg_zodiac_item
            )
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZodiacViewHolder {
        val binding = ItemZodiacBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ZodiacViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ZodiacViewHolder, position: Int) {
        holder.bind(signs[position])
    }

    override fun getItemCount() = signs.size
}