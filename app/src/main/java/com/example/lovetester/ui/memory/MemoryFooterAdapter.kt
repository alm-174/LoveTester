package com.example.lovetester.ui.memory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.databinding.ItemMemoryFooterBinding

internal class MemoryFooterAdapter(
    private val onAddMemory: () -> Unit
) : RecyclerView.Adapter<MemoryFooterAdapter.FooterViewHolder>() {

    private var visible = false

    fun setVisible(visible: Boolean) {
        if (this.visible == visible) return
        this.visible = visible
        if (visible) notifyItemInserted(0) else notifyItemRemoved(0)
    }

    override fun getItemCount(): Int = if (visible) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = ItemMemoryFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.btnAddMemoryFooter.setOnClickListener { onAddMemory() }
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) = Unit

    class FooterViewHolder(binding: ItemMemoryFooterBinding) : RecyclerView.ViewHolder(binding.root)
}
