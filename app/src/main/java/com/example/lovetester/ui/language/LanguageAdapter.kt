package com.example.lovetester.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.databinding.ItemLanguageBinding
import com.example.lovetester.R
class LanguageAdapter(
    private val languages: List<Language>,
    private val onLanguageClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageHolder>(){

    private var selectedPosition = 0
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

        holder.binding.apply {
            tvLanguageName.setText(language.nameRes)
            ivFlag.setImageResource(language.flagRes)

            languageItem.setBackgroundResource(
                if (position == selectedPosition) {
                    R.drawable.bg_language_selected
                } else {
                    R.drawable.bg_rectangle_white
                }
            )

            root.setOnClickListener {
                val clickedPosition = holder.bindingAdapterPosition

                if (clickedPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                if (clickedPosition != selectedPosition) {
                    val oldPosition = selectedPosition

                    selectedPosition = clickedPosition

                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                }

                onLanguageClick(languages[clickedPosition])
            }
        }
    }

    override fun getItemCount(): Int {
        return languages.size
    }

    fun getSelectedLanguage(): Language {
        return languages[selectedPosition]
    }


}