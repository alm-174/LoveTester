package com.example.lovetester.ui.background

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.example.lovetester.databinding.ItemBackgroundBinding
import java.io.File

/**
 * Grid of backgrounds. Like LanguageAdapter, the highlighted item comes from outside
 * ([selectedId]); this adapter only draws it (root.isSelected + selector foreground).
 */
class BackgroundAdapter(
    private val items: List<BackgroundItem>,
    selectedId: String?,
    private val onItemClick: (BackgroundItem) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.BackgroundViewHolder>() {

    var selectedId: String? = selectedId
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, items.size)
        }

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

                    ivBackground.load(item.drawableRes)
                }

                is BackgroundItem.Custom -> {
                    ivBackground.visibility = View.VISIBLE
                    ivAdd.visibility = View.GONE

                    // Coil: off the main thread, downsampled to the cell size.
                    ivBackground.load(File(item.path))
                }
            }

            root.isSelected = item.storedId() == selectedId

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

/** The id stored in settings for this item (null for the "add photo" cell). */
internal fun BackgroundItem.storedId(): String? = when (this) {
    BackgroundItem.AddPhoto -> null
    is BackgroundItem.BuiltIn -> id
    is BackgroundItem.Custom -> "custom:$path"
}
