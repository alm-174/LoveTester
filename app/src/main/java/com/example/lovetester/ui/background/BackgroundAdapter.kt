package com.example.lovetester.ui.background

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lovetester.databinding.ItemBackgroundBinding
import androidx.recyclerview.widget.GridLayoutManager
import android.net.Uri
import java.io.File

class BackgroundAdapter(
    private val items: List<BackgroundItem>,
    private val onItemClick: (BackgroundItem) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.BackgroundViewHolder>() {

    class BackgroundViewHolder(
        val binding: ItemBackgroundBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BackgroundViewHolder {
        val binding = ItemBackgroundBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BackgroundViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BackgroundViewHolder,
        position: Int
    ) {
        val item = items[position]

        holder.binding.apply {
            when (item) {
                BackgroundItem.AddPhoto -> {
                    ivBackground.visibility = View.GONE
                    ivAdd.visibility = View.VISIBLE
                }

                is BackgroundItem.BuiltIn -> {
                    ivBackground.visibility = View.VISIBLE
                    ivAdd.visibility = View.GONE

                    ivBackground.setImageResource(item.drawableRes)
                }

                is BackgroundItem.Custom -> {
                    ivBackground.visibility = View.VISIBLE
                    ivAdd.visibility = View.GONE

                    ivBackground.setImageURI(
                        Uri.fromFile(File(item.path))
                    )
                }
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

}