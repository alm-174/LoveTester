package com.example.lovetester.ui.memory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.databinding.ItemAddMemoryBinding

/**
 * Single-item footer card ("+ Add memory") shown at the end of the memory list, same size and
 * corner radius as a memory card. Hidden while the list is empty (the empty state has its own +).
 */
internal class AddMemoryAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<AddMemoryAdapter.AddMemoryViewHolder>() {

    var isShown: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) notifyItemInserted(0) else notifyItemRemoved(0)
        }

    override fun getItemCount(): Int = if (isShown) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddMemoryViewHolder {
        val binding = ItemAddMemoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.cardAddMemory.setOnClickListener { onClick() }
        return AddMemoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddMemoryViewHolder, position: Int) = Unit

    class AddMemoryViewHolder(binding: ItemAddMemoryBinding) : RecyclerView.ViewHolder(binding.root)
}
