
package com.example.lovetester.ui.memory

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
import androidx.recyclerview.widget.LinearLayoutManager
@AndroidEntryPoint
class MemoryListFragment : Fragment(R.layout.fragment_memory_list) {

    private var _binding: FragmentMemoryListBinding? = null

    private val binding: FragmentMemoryListBinding
        get() = requireNotNull(_binding)

    private val viewModel: MemoryViewModel by activityViewModels()

    private val memoryAdapter = MemoryAdapter()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMemoryListBinding.bind(view)

        binding.rvMemories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memoryAdapter
        }

        observeUiState()

        val addMemory = View.OnClickListener { openAddMemory() }
        binding.btnAddMemory.setOnClickListener(addMemory)
        binding.btnAddMoreMemory.setOnClickListener(addMemory)
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
        binding.btnAddMoreMemory.isVisible = state is MemoryUiState.Success && state.memories.isNotEmpty()
        when (state) {

            is MemoryUiState.Loading -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false
            }

            is MemoryUiState.Success -> {
                val isEmpty = state.memories.isEmpty()

                binding.layoutEmptyState.isVisible = isEmpty
                binding.rvMemories.isVisible = !isEmpty

                memoryAdapter.submitList(state.memories)
            }

            is MemoryUiState.Error -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false

                Toast.makeText(
                    requireContext(),
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openAddMemory() {
        val manager = parentFragmentManager
        if (manager.isStateSaved || manager.findFragmentById(id) !== this) return
        binding.btnAddMemory.isEnabled = false
        binding.btnAddMoreMemory.isEnabled = false
        manager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                id,
                AddMemoryFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        binding.rvMemories.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
