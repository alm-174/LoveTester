
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@AndroidEntryPoint
class MemoryListFragment : Fragment(R.layout.fragment_memory_list) {

    private val binding by viewBinding(FragmentMemoryListBinding::bind)

    private val viewModel: MemoryViewModel by activityViewModels()

    private val memoryAdapter = MemoryAdapter(
        onDeleteRequest = { MemoryEntity ->
            confirmDelete(MemoryEntity)
        }
    )

    private val addMemoryAdapter = AddMemoryAdapter(::openAddMemory)
    private val listAdapter = ConcatAdapter(memoryAdapter, addMemoryAdapter)
    private var openingAddMemory = false

    override fun onResume() {
        super.onResume()
        openingAddMemory = false
        binding.btnAddMemory.isEnabled = true
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        openingAddMemory = false

        binding.rvMemories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = listAdapter
        }

        observeUiState()

        applyInsets()

        val addMemory = View.OnClickListener { openAddMemory() }
        binding.btnAddMemory.setOnClickListener(addMemory)
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

                binding.layoutEmptyState.isVisible = isEmpty
                binding.rvMemories.isVisible = !isEmpty

                memoryAdapter.submitList(state.memories) {
                    addMemoryAdapter.isShown = memoryAdapter.currentList.isNotEmpty()
                }
            }

            is MemoryUiState.Error -> {
                binding.layoutEmptyState.isVisible = false
                binding.rvMemories.isVisible = false
                binding.tvErrorMessage.setText(state.messageRes)
            }
        }
    }

    private fun applyInsets() {
        val rootStartLeft = binding.root.paddingLeft
        val rootStartTop = binding.root.paddingTop
        val rootStartRight = binding.root.paddingRight
        val rvStartBottom = binding.rvMemories.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBars = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )

            // Đẩy tiêu đề "Memory" và nút "+" xuống dưới thanh trạng thái (nền bg_splash vẫn tràn lên trên)
            view.setPadding(
                rootStartLeft + statusBars.left,
                rootStartTop + statusBars.top,
                rootStartRight + statusBars.right,
                view.paddingBottom
            )

            // Cộng thêm chiều cao thanh điều hướng hệ thống vào đáy danh sách
            binding.rvMemories.setPadding(
                binding.rvMemories.paddingLeft,
                binding.rvMemories.paddingTop,
                binding.rvMemories.paddingRight,
                rvStartBottom + navBars.bottom
            )

            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }
    private fun openAddMemory() {
        if (openingAddMemory) return
        openingAddMemory = true
        binding.btnAddMemory.isEnabled = false
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
}
