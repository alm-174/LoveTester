package com.example.lovetester.ui.memory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.lovetester.R
import com.example.lovetester.data.local.database.MemoryEntity
import com.example.lovetester.databinding.ItemMemoryBinding
import java.io.File

class MemoryAdapter(
    private val onDeleteRequest: (MemoryEntity) -> Unit
) :
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
        private val binding: ItemMemoryBinding,
        private val onDeleteRequest: (MemoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var deleteActionId = View.NO_ID

        fun clearPhoto() {
            binding.ivMemoryPhoto.setImageDrawable(null)
        }

        fun bind(memory: MemoryEntity) {
            binding.tvMemoryCardTitle.text = memory.title

            // 2. Hiển thị ngày kỷ niệm
            binding.tvMemoryCardDate.text = memory.date

            // 3. Hiển thị ảnh kỷ niệm. Coil decodes off the main thread, downsamples to the view
            // size and cancels the request itself when the view is reused or detached.
            binding.ivMemoryPhoto.load(File(memory.imagePath))

            binding.root.setOnLongClickListener {
                onDeleteRequest(memory)
                true
            }

            // TalkBack users cannot long-press: expose the same action in the actions menu.
            if (deleteActionId != View.NO_ID) {
                ViewCompat.removeAccessibilityAction(binding.root, deleteActionId)
            }
            deleteActionId = ViewCompat.addAccessibilityAction(
                binding.root,
                binding.root.context.getString(R.string.memory_delete_button)
            ) { _, _ ->
                onDeleteRequest(memory)
                true
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

        return MemoryViewHolder(
            binding,
            onDeleteRequest
        )
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
