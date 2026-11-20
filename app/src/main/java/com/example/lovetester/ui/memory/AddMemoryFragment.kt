package com.example.lovetester.ui.memory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentAddMemoryBinding
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddMemoryFragment : Fragment(R.layout.fragment_add_memory) {

    private var _binding: FragmentAddMemoryBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MemoryViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null
    private var selectedDate: LocalDate? = null
    private var previewJob: Job? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val resolver = requireContext().contentResolver
                if (uri != selectedImageUri) {
                    releaseImagePermission()
                    runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                }
                selectedImageUri = uri
                if (_binding != null) {
                    showError(null)
                    renderPhoto()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedImageUri = savedInstanceState?.getString(KEY_IMAGE)?.let(Uri::parse)
        selectedDate = savedInstanceState?.getString(KEY_DATE)?.let(LocalDate::parse)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddMemoryBinding.bind(view)
        binding.root.isClickable = true
        // EditText already restores its text using its stable XML ID.
        renderPhoto()
        renderDate()
        observeDatePickerResult()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            closeForm()
        }
        binding.btnBack.setOnClickListener { closeForm() }
        binding.cardMemoryPhoto.setOnClickListener {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.layoutMemoryDate.setOnClickListener {
            if (childFragmentManager.isStateSaved ||
                childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null
            ) return@setOnClickListener
            LoveDatePickerDialog.newInstance(selectedDate ?: LocalDate.now())
                .showNow(childFragmentManager, DATE_PICKER_TAG)
        }
        binding.btnSaveMemory.setOnClickListener { saveMemory() }

        renderSaving(viewModel.saveState.value == MemorySaveState.Saving)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.saveState.collect { state ->
                    renderSaving(state == MemorySaveState.Saving)
                    when (state) {
                        MemorySaveState.Idle, MemorySaveState.Saving -> Unit
                        MemorySaveState.Error ->
                            showError("Could not save memory. Check the photo or try again.")
                        MemorySaveState.Success -> binding.root.post {
                            if (_binding != null && viewModel.saveState.value == MemorySaveState.Success) {
                                closeForm()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveMemory() {
        val title = binding.etMemoryTitle.text.toString().trim()
        if (title.isBlank() || title.length > 100) {
            binding.etMemoryTitle.error = if (title.isBlank()) "Enter a title" else "Use at most 100 characters"
            binding.etMemoryTitle.requestFocus()
            return
        }
        val date = selectedDate
        if (date == null) {
            showError("Select a date for this memory.")
            return
        }
        val image = selectedImageUri
        if (image == null) {
            showError("Choose a photo for this memory.")
            return
        }
        showError(null)
        viewModel.saveMemory(title, date.toString(), image)
        renderSaving(true)
    }

    private fun observeDatePickerResult() {
        childFragmentManager.setFragmentResultListener(
            LoveDatePickerDialog.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val date = bundle.getString(LoveDatePickerDialog.RESULT_DATE)
                ?: return@setFragmentResultListener
            selectedDate = LocalDate.parse(date)
            showError(null)
            renderDate()
        }
    }

    private fun renderDate() {
        selectedDate?.let { binding.tvMemoryDate.text = it.format(dateFormatter) }
    }

    private fun renderPhoto() {
        previewJob?.cancel()
        binding.ivMemoryPreview.setImageDrawable(null)
        binding.layoutAddPhoto.isVisible = true
        val uri = selectedImageUri ?: return
        val resolver = requireContext().contentResolver
        previewJob = viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = decodeMemoryBitmap(resolver, uri)
            if (bitmap == null) {
                releaseImagePermission()
                selectedImageUri = null
                showError("Cannot read this photo. Choose it again or select another one.")
            } else {
                binding.ivMemoryPreview.setImageBitmap(bitmap)
                binding.layoutAddPhoto.isVisible = false
            }
        }
    }

    private fun renderSaving(saving: Boolean) {
        binding.btnSaveMemory.isEnabled = !saving
        binding.btnBack.isEnabled = !saving
        binding.etMemoryTitle.isEnabled = !saving
        binding.cardMemoryPhoto.isEnabled = !saving
        binding.layoutMemoryDate.isEnabled = !saving
        binding.btnSaveMemory.text = if (saving) "Saving…" else "Save"
    }

    private fun showError(message: String?) {
        binding.tvMemoryFormError.text = message
        binding.tvMemoryFormError.isVisible = message != null
        if (message != null) {
            binding.tvMemoryFormError.post {
                _binding?.tvMemoryFormError?.let { error ->
                    error.requestRectangleOnScreen(android.graphics.Rect(0, 0, error.width, error.height))
                }
            }
        }
    }

    private fun closeForm() {
        val manager = parentFragmentManager
        if (viewModel.saveState.value == MemorySaveState.Saving ||
            manager.isStateSaved || manager.findFragmentById(id) !== this
        ) return

        val resolver = requireContext().contentResolver
        val uri = selectedImageUri
        if (manager.popBackStackImmediate()) {
            if (uri != null) {
                runCatching {
                    resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            viewModel.clearSaveResult()
        }
    }

    private fun releaseImagePermission() {
        val uri = selectedImageUri ?: return
        runCatching {
            requireContext().contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_IMAGE, selectedImageUri?.toString())
        outState.putString(KEY_DATE, selectedDate?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        previewJob?.cancel()
        previewJob = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DATE_PICKER_TAG = "love_date_picker"
        const val KEY_IMAGE = "memory_image"
        const val KEY_DATE = "memory_date"
    }
}
