package com.example.lovetester.ui.lovetest.horoscope

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivityHoroscopeTestBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.collectOn
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.common.showOnce
import com.example.lovetester.ui.lovetest.result.ResultActivity

class HoroscopeTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHoroscopeTestBinding
    private val viewModel: HoroscopeTestViewModel by viewModels()
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        binding = ActivityHoroscopeTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        setupResultListeners()
        setupClicks()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }

    private fun setupResultListeners() {
        supportFragmentManager.setFragmentResultListener(
            REQUEST_MY_SIGN, this
        ) { _, bundle ->
            bundle.toZodiacSign()?.let { viewModel.setMySign(it) }
        }

        supportFragmentManager.setFragmentResultListener(
            REQUEST_PARTNER_SIGN, this
        ) { _, bundle ->
            bundle.toZodiacSign()?.let { viewModel.setPartnerSign(it) }
        }
    }

    private fun Bundle.toZodiacSign(): ZodiacSign? =
        getString(HoroscopePickerBottomSheet.RESULT_SIGN)?.let { ZodiacSign.valueOf(it) }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }

        binding.layoutYourZodiac.setOnClickListener {
            openPicker(REQUEST_MY_SIGN, viewModel.uiState.value.mySign)
        }
        binding.layoutPartnerZodiac.setOnClickListener {
            openPicker(REQUEST_PARTNER_SIGN, viewModel.uiState.value.partnerSign)
        }

        binding.btnLoveTest.setOnClickListener {
            if (isNavigating) return@setOnClickListener

            val state = viewModel.uiState.value
            val percentage = viewModel.calculatePercent()

            isNavigating = true
            startActivity(
                ResultActivity.newHoroscopeIntent(
                    context = this,
                    mySign = state.mySign,
                    partnerSign = state.partnerSign,
                    percentage = percentage
                )
            )
        }
    }

    private fun openPicker(requestKey: String, current: ZodiacSign) {
        supportFragmentManager.showOnce(
            HoroscopePickerBottomSheet.newInstance(requestKey, current),
            HoroscopePickerBottomSheet.TAG
        )
    }

    private fun observeState() {
        viewModel.uiState.collectOn(this) { state ->
            binding.ivYourZodiac.setImageResource(state.mySign.iconRes)
            binding.tvYourZodiac.setText(state.mySign.nameRes)

            binding.ivPartnerZodiac.setImageResource(state.partnerSign.iconRes)
            binding.tvPartnerZodiac.setText(state.partnerSign.nameRes)
        }
    }

    companion object {
        private const val REQUEST_MY_SIGN = "request_my_sign"
        private const val REQUEST_PARTNER_SIGN = "request_partner_sign"
    }
}