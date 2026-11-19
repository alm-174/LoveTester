package com.example.lovetester.ui.memory

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.data.local.database.MemoryEntity
import com.example.lovetester.databinding.ItemMemoryBinding
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MemoryAdapter :
    ListAdapter<MemoryEntity, MemoryAdapter.MemoryViewHolder>(
        DIFF_CALLBACK
    ) {
    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<MemoryEntity>() {

                override fun areItemsTheSame(
                    oldItem: MemoryEntity,
                    newItem: MemoryEntity
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: MemoryEntity,
                    newItem: MemoryEntity
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    class MemoryViewHolder(
        private val binding: ItemMemoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var imageJob: Job? = null

        fun clearPhoto() {
            imageJob?.cancel()
            imageJob = null
            binding.ivMemoryPhoto.setImageDrawable(null)
        }

        fun bind(memory: MemoryEntity) {
            binding.tvMemoryCardTitle.text = memory.title

            // 2. Hiển thị ngày kỷ niệm
            binding.tvMemoryCardDate.text = memory.date

            // 3. Hiển thị ảnh kỷ niệm
            clearPhoto()
            val resolver = binding.root.context.contentResolver
            imageJob = CoroutineScope(Dispatchers.Main.immediate).launch {
                val image = decodeMemoryBitmap(resolver, Uri.fromFile(File(memory.imagePath)))
                binding.ivMemoryPhoto.setImageBitmap(image)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int):MemoryViewHolder{
        val binding = ItemMemoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MemoryViewHolder(binding)
    }

    override fun onViewRecycled(holder: MemoryViewHolder) {
        holder.clearPhoto()
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(
        holder: MemoryViewHolder,
        position: Int
    ) {
        val memory = getItem(position)
        holder.bind(memory)
    }
}