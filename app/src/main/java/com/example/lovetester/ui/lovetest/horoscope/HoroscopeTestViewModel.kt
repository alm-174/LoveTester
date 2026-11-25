package com.example.lovetester.ui.lovetest.horoscope

import androidx.lifecycle.ViewModel
import com.example.lovetester.ui.lovetest.domain.LoveTestCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HoroscopeUiState(
    val mySign: ZodiacSign = ZodiacSign.ARIES,
    val partnerSign: ZodiacSign = ZodiacSign.ARIES
)

class HoroscopeTestViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HoroscopeUiState())
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    fun setMySign(sign: ZodiacSign) {
        _uiState.update { it.copy(mySign = sign) }
    }

    fun setPartnerSign(sign: ZodiacSign) {
        _uiState.update { it.copy(partnerSign = sign) }
    }

    fun calculatePercent(): Int {
        val state = _uiState.value
        return LoveTestCalculator.calculateNameTest(
            "zodiac:${state.mySign.name}",
            "zodiac:${state.partnerSign.name}"
        )
    }
}