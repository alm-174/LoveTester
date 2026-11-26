package com.example.lovetester.ui.startdate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.R
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivityStartDateBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.dialog.datepicker.LoveDatePickerDialog
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.common.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class StartDateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartDateBinding

    @Inject
    lateinit var repository: LoveSettingsRepository

    private var selectedDate: LocalDate? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        binding = ActivityStartDateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        // Nhận kết quả từ Dialog
        supportFragmentManager.setFragmentResultListener(
            LoveDatePickerDialog.REQUEST_KEY,
            this
        ) { _, bundle ->
            val dateString = bundle.getString(LoveDatePickerDialog.RESULT_DATE)
            selectedDate = dateString?.let(LocalDate::parse)
            updateSelectedDate()
        }

        // Giữ ngày đang chỉnh sửa khi xoay màn hình
        val restoredDate = savedInstanceState
            ?.getString(KEY_PENDING_DATE)
            ?.let(LocalDate::parse)

        if (restoredDate != null) {
            selectedDate = restoredDate
            updateSelectedDate()
        } else {
            loadSavedDate()
        }

        setupListeners()
    }

    private fun loadSavedDate() {
        lifecycleScope.launch {
            selectedDate = repository.loveStartDate.first()
            updateSelectedDate()
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutDateOfLove.setOnClickListener {
            showDatePicker()
        }

        binding.btnDone.setOnClickListener {
            saveAndFinish()
        }
    }

    private fun showDatePicker() {
        val dialog = LoveDatePickerDialog.newInstance(
            selectedDate ?: LocalDate.now()
        )
        supportFragmentManager.showOnce(dialog, "LoveDatePickerDialog")
    }

    private fun updateSelectedDate() {
        binding.tvSelectedDate.text =
            selectedDate?.format(dateFormatter)
                ?: getString(R.string.common_select_date)
    }

    private fun saveAndFinish() {
        val date = selectedDate ?: return
        binding.btnDone.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.setLoveStartDate(date)
                finish()
            } catch (e: Exception) {
                binding.btnDone.isEnabled = true
                toast(R.string.start_date_save_error)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_PENDING_DATE, selectedDate?.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_PENDING_DATE = "pending_date"
    }
}