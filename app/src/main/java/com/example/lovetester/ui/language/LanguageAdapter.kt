package com.example.lovetester.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.R
import com.example.lovetester.databinding.ItemLanguageBinding

/**
 * Shows the language list. The highlighted item lives in [LanguageViewModel]; this adapter only
 * draws it ([selectedId]) and reports clicks.
 */
class LanguageAdapter(
    private val languages: List<Language>,
    selectedId: String,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageHolder>() {

    var selectedId: String = selectedId
        set(value) {
            if (field == value) return
            val oldIndex = languages.indexOfFirst { it.id == field }
            field = value
            val newIndex = languages.indexOfFirst { it.id == value }
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)
        }

    class LanguageHolder(
        val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LanguageHolder,
        position: Int
    ) {
        val language = languages[position]
        val isSelected = language.id == selectedId

        holder.binding.apply {
            tvLanguageName.setText(language.nameRes)
            ivFlag.setImageResource(language.flagRes)

            root.isSelected = isSelected
            languageItem.setBackgroundResource(
                if (isSelected) R.drawable.bg_language_selected
                else R.drawable.bg_rectangle_white
            )

            root.setOnClickListener {
                val clickedPosition = holder.bindingAdapterPosition
                if (clickedPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                onLanguageClick(languages[clickedPosition])
            }
        }
    }

    override fun getItemCount(): Int = languages.size
}
