package com.example.lovetester.ui.memory

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityAddMemoryBinding
import com.example.lovetester.ui.common.applySystemBarsAndImePadding
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.common.hideKeyboard
import com.example.lovetester.ui.common.onImeDone
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddMemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMemoryBinding
    private val viewModel: MemoryViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var selectedDate: LocalDate? = null
    private var previewJob: Job? = null
    private var discardDialog: AlertDialog? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                if (uri != selectedImageUri) {
                    releaseImagePermission()
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }
                selectedImageUri = uri
                showError(null)
                renderPhoto()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        binding = ActivityAddMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsAndImePadding()

        selectedImageUri = savedInstanceState?.getString(KEY_IMAGE)?.let(Uri::parse)
        selectedDate = savedInstanceState?.getString(KEY_DATE)?.let(LocalDate::parse)

        renderPhoto()
        renderDate()
        observeDatePickerResult()

        onBackPressedDispatcher.addCallback(this) {
            requestClose()
        }
        binding.btnBack.setOnClickListener { requestClose() }
        binding.cardMemoryPhoto.setOnClickListener {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.layoutMemoryDate.setOnClickListener {
            if (supportFragmentManager.isStateSaved ||
                supportFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null
            ) return@setOnClickListener
            LoveDatePickerDialog.newInstance(selectedDate ?: LocalDate.now())
                .showNow(supportFragmentManager, DATE_PICKER_TAG)
        }
        binding.etMemoryTitle.onImeDone()
        binding.btnSaveMemory.setOnClickListener {
            binding.etMemoryTitle.hideKeyboard()
            saveMemory()
        }

        renderSaving(viewModel.saveState.value == MemorySaveState.Saving)
        viewModel.saveState.collectOn(this, Lifecycle.State.RESUMED) { state ->
            renderSaving(state == MemorySaveState.Saving)
            when (state) {
                MemorySaveState.Idle, MemorySaveState.Saving -> Unit
                MemorySaveState.Error ->
                    showError(getString(R.string.add_memory_error_save))
                MemorySaveState.Success -> binding.root.post {
                    if (!isFinishing && viewModel.saveState.value == MemorySaveState.Success) {
                        closeForm()
                    }
                }
            }
        }
    }

    private fun saveMemory() {
        val title = binding.etMemoryTitle.text.toString().trim()
        if (title.isBlank() || title.length > 100) {
            binding.etMemoryTitle.error = getString(
                if (title.isBlank()) R.string.add_memory_error_title_empty
                else R.string.add_memory_error_title_too_long
            )
            binding.etMemoryTitle.requestFocus()
            return
        }
        val date = selectedDate
        if (date == null) {
            showError(getString(R.string.add_memory_error_date))
            return
        }
        val image = selectedImageUri
        if (image == null) {
            showError(getString(R.string.add_memory_error_photo))
            return
        }
        showError(null)
        viewModel.saveMemory(title, date.toString(), image)
        renderSaving(true)
    }

    private fun observeDatePickerResult() {
        supportFragmentManager.setFragmentResultListener(
            LoveDatePickerDialog.REQUEST_KEY,
            this
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
        previewJob = lifecycleScope.launch {
            val bitmap = decodeMemoryBitmap(contentResolver, uri)
            if (bitmap == null) {
                releaseImagePermission()
                selectedImageUri = null
                showError(getString(R.string.add_memory_error_read_photo))
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
        binding.btnSaveMemory.setText(
            if (saving) R.string.common_saving else R.string.common_save
        )
    }

    private fun showError(message: String?) {
        binding.tvMemoryFormError.text = message
        binding.tvMemoryFormError.isVisible = message != null
        if (message != null) {
            binding.tvMemoryFormError.post {
                val error = binding.tvMemoryFormError
                error.requestRectangleOnScreen(
                    android.graphics.Rect(0, 0, error.width, error.height)
                )
            }
        }
    }

    private fun hasUnsavedChanges(): Boolean =
        binding.etMemoryTitle.text.isNotBlank() || selectedImageUri != null || selectedDate != null

    private fun requestClose() {
        if (viewModel.saveState.value == MemorySaveState.Saving) return
        if (!hasUnsavedChanges()) {
            closeForm()
            return
        }
        if (discardDialog?.isShowing == true) return

        discardDialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_memory_discard_title)
            .setMessage(R.string.add_memory_discard_message)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.add_memory_discard_confirm) { _, _ -> closeForm() }
            .show()
    }

    private fun closeForm() {
        if (viewModel.saveState.value == MemorySaveState.Saving || isFinishing) return

        releaseImagePermission()
        viewModel.clearSaveResult()
        finish()
    }

    private fun releaseImagePermission() {
        val uri = selectedImageUri ?: return
        runCatching {
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_IMAGE, selectedImageUri?.toString())
        outState.putString(KEY_DATE, selectedDate?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        previewJob?.cancel()
        previewJob = null
        discardDialog?.dismiss()
        discardDialog = null
        super.onDestroy()
    }

    private companion object {
        const val DATE_PICKER_TAG = "love_date_picker"
        const val KEY_IMAGE = "memory_image"
        const val KEY_DATE = "memory_date"
    }
}