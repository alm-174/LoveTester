
package com.example.lovetester.ui.memory

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentMemoryListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lovetester.data.local.database.MemoryEntity

@AndroidEntryPoint
class MemoryListFragment : Fragment(R.layout.fragment_memory_list) {

    private var _binding: FragmentMemoryListBinding? = null

    private val binding: FragmentMemoryListBinding
        get() = requireNotNull(_binding)

    private val viewModel: MemoryViewModel by activityViewModels()

    private val memoryAdapter = MemoryAdapter(
        onDeleteRequest = { MemoryEntity ->
            confirmDelete(MemoryEntity)
        }
    )

    private val footerAdapter = MemoryFooterAdapter(::openAddMemory)
    private val listAdapter = ConcatAdapter(memoryAdapter, footerAdapter)
    private var openingAddMemory = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMemoryListBinding.bind(view)
        openingAddMemory = false

        binding.rvMemories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }

        observeUiState()

        val addMemory = View.OnClickListener { openAddMemory() }
        binding.btnAddMemory.setOnClickListener(addMemory)
        binding.root.isClickable = true
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: MemoryUiState) {
        when (state) {

            is MemoryUiState.Loading -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false
            }

            is MemoryUiState.Success -> {
                val isEmpty = state.memories.isEmpty()

                binding.layoutEmptyState.isVisible = isEmpty
                binding.rvMemories.isVisible = !isEmpty

                memoryAdapter.submitList(state.memories) {
                    footerAdapter.setVisible(memoryAdapter.currentList.isNotEmpty())
                }
            }

            is MemoryUiState.Error -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false

                Toast.makeText(
                    requireContext(),
                    getString(state.messageRes),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openAddMemory() {
        val manager = parentFragmentManager
        if (openingAddMemory || manager.isStateSaved || manager.findFragmentById(id) !== this) return
        openingAddMemory = true
        binding.btnAddMemory.isEnabled = false
        manager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                id,
                AddMemoryFragment()
            )
            .addToBackStack(null)
            .commit()
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
        _binding = null
        super.onDestroyView()
    }
}
