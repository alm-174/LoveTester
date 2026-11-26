
package com.example.lovetester.ui.memory

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentMemoryListBinding
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.data.local.database.MemoryEntity
import android.content.Intent
import com.example.lovetester.ui.common.applySystemBarsPadding

@AndroidEntryPoint
class MemoryListFragment : Fragment(R.layout.fragment_memory_list) {

    private val binding by viewBinding(FragmentMemoryListBinding::bind)

    private val viewModel: MemoryViewModel by activityViewModels()

    private val memoryAdapter = MemoryAdapter(
        onDeleteRequest = { MemoryEntity ->
            confirmDelete(MemoryEntity)
        }
    )

    private val footerAdapter = MemoryFooterAdapter(::openAddMemory)
    private val listAdapter = ConcatAdapter(memoryAdapter, footerAdapter)
    private var openingAddMemory = false

    override fun onResume() {
        super.onResume()
        openingAddMemory = false
        binding.btnAddMemory.isEnabled = true
        binding.btnAddMemoryHeader.isEnabled = true
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding()

        openingAddMemory = false

        binding.rvMemories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }

        observeUiState()

        val addMemory = View.OnClickListener { openAddMemory() }
        binding.btnAddMemory.setOnClickListener(addMemory)
        binding.btnAddMemoryHeader.setOnClickListener(addMemory)
        binding.root.isClickable = true
        binding.btnRetryMemories.setOnClickListener { viewModel.retry() }
    }

    private fun observeUiState() {
        viewModel.uiState.collectOn(viewLifecycleOwner, action = ::render)
    }

    private fun render(state: MemoryUiState) {
        binding.progressMemories.isVisible = state is MemoryUiState.Loading
        binding.layoutErrorState.isVisible = state is MemoryUiState.Error

        when (state) {

            is MemoryUiState.Loading -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false
            }

            is MemoryUiState.Success -> {
                val isEmpty = state.memories.isEmpty()
                val count = state.memories.size

                binding.layoutEmptyState.isVisible = isEmpty
                binding.rvMemories.isVisible = !isEmpty

                memoryAdapter.submitList(state.memories) {
                    footerAdapter.setVisible(count in 1 until HEADER_ADD_THRESHOLD)
                    binding.btnAddMemoryHeader.isVisible = count >= HEADER_ADD_THRESHOLD
                }
            }

            is MemoryUiState.Error -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false
                binding.tvErrorMessage.setText(state.messageRes)
            }
        }
    }

    private fun openAddMemory() {
        if (openingAddMemory) return
        openingAddMemory = true
        binding.btnAddMemory.isEnabled = false
        binding.btnAddMemoryHeader.isEnabled = false
        startActivity(Intent(requireContext(), AddMemoryActivity::class.java))
    }

    private fun confirmDelete(memory : MemoryEntity){
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.memory_delete_title)
            .setMessage(getString(R.string.memory_delete_message, memory.title))
            .setNegativeButton(R.string.common_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.memory_delete_button) { _, _ ->
                // Bài tiếp theo: gọi ViewModel để xóa
                viewModel.delete(memory)
            }
            .show()
    }

    override fun onDestroyView() {
        binding.rvMemories.adapter = null
        super.onDestroyView()
    }

    private companion object {
        const val HEADER_ADD_THRESHOLD = 5
    }
}
