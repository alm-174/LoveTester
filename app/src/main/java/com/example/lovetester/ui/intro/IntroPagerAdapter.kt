package com.example.lovetester.ui.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.databinding.ItemIntroPageBinding
import com.example.lovetester.ui.language.LanguageAdapter.LanguageHolder

class IntroPagerAdapter(
    private val pages: List<IntroPage>
) : RecyclerView.Adapter<IntroPagerAdapter.IntroViewHolder>() {

    class IntroViewHolder(
        val binding: ItemIntroPageBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
        val binding = ItemIntroPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return IntroViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: IntroViewHolder,
        position: Int
    ){
        val page = pages[position]

        holder.binding.apply{
            ivIllustration.setImageResource(page.imageRes)
            tvTitle.setText(page.titleRes)
            tvDescription.setText(page.descriptionRes)
        }
    }
}