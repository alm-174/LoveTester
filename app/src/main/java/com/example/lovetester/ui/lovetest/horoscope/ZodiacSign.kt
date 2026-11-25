package com.example.lovetester.ui.lovetest.horoscope

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.lovetester.R

enum class ZodiacSign(
    @param:StringRes val nameRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    ARIES(R.string.zodiac_aries, R.drawable.zodiac_aries),
    TAURUS(R.string.zodiac_taurus, R.drawable.zodiac_taurus),
    GEMINI(R.string.zodiac_gemini, R.drawable.zodiac_gemini),
    CANCER(R.string.zodiac_cancer, R.drawable.zodiac_cancer),
    LEO(R.string.zodiac_leo, R.drawable.zodiac_leo),
    VIRGO(R.string.zodiac_virgo, R.drawable.zodiac_virgo),
    LIBRA(R.string.zodiac_libra, R.drawable.zodiac_libra),
    SCORPIO(R.string.zodiac_scorpio, R.drawable.zodiac_scorpio),
    SAGITTARIUS(R.string.zodiac_sagittarius, R.drawable.zodiac_sagittarius),
    CAPRICORN(R.string.zodiac_capricorn, R.drawable.zodiac_capricorn),
    AQUARIUS(R.string.zodiac_aquarius, R.drawable.zodiac_aquarius),
    PISCES(R.string.zodiac_pisces, R.drawable.zodiac_pisces)
}
