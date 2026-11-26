package com.example.lovetester.ui.lovetest.result

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.addCallback
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityResultBinding
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.home.HomeActivity
import com.example.lovetester.ui.lovetest.horoscope.ZodiacSign

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        val percentage = intent.getIntExtra(EXTRA_PERCENTAGE, -1)
        val signOne = intent.getStringExtra(EXTRA_SIGN_ONE)
            ?.let { runCatching { ZodiacSign.valueOf(it) }.getOrNull() }
        val signTwo = intent.getStringExtra(EXTRA_SIGN_TWO)
            ?.let { runCatching { ZodiacSign.valueOf(it) }.getOrNull() }
        val isHoroscope = signOne != null && signTwo != null

        val personOne = if (isHoroscope) getString(signOne!!.nameRes) else intent.getStringExtra(EXTRA_PERSON_ONE)
        val personTwo = if (isHoroscope) getString(signTwo!!.nameRes) else intent.getStringExtra(EXTRA_PERSON_TWO)

        if (percentage !in 0..100 || personOne == null || personTwo == null) {
            finish()
            return
        }

        binding.tvPersonOne.text = personOne
        binding.tvPersonTwo.text = personTwo
        binding.tvPercentage.text = getString(R.string.percent_format, percentage)

        if (isHoroscope) {
            bindZodiacIcons(signOne!!, signTwo!!)
        }

        val message = getString(
            if (isHoroscope) getHoroscopeMessageRes(percentage)
            else getNameTestMessageRes(percentage)
        )
        binding.tvResultMessage.text = message

        setupButtons(personOne, personTwo, percentage, message)

        onBackPressedDispatcher.addCallback(this) {
            navigateToTestMenu()
        }
    }

    private fun setupButtons(
        personOne: String,
        personTwo: String,
        percentage: Int,
        message: String
    ) {
        binding.btnBack.setOnClickListener {
            navigateToTestMenu()
        }

        binding.btnRetry.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            val percentText = getString(R.string.percent_format, percentage)
            val shareText = "$personOne ❤ $personTwo: $percentText\n\n$message"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        }
    }

    private fun navigateToTestMenu() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(HomeActivity.EXTRA_TAB, HomeActivity.TAB_LOVE_TEST)
            }
        )
        finish()
    }

    private fun bindZodiacIcons(signOne: ZodiacSign, signTwo: ZodiacSign) {
        binding.ivPersonOne.setImageResource(signOne.iconRes)
        binding.ivPersonTwo.setImageResource(signTwo.iconRes)

        val pink = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primary)
        )
        binding.ivPersonOne.imageTintList = pink
        binding.ivPersonTwo.imageTintList = pink
    }

    @StringRes
    private fun getNameTestMessageRes(percentage: Int): Int = when (percentage) {
        in 0..20 -> R.string.name_test_message_0_20
        in 21..50 -> R.string.name_test_message_21_50
        in 51..80 -> R.string.name_test_message_51_80
        else -> R.string.name_test_message_81_100
    }

    @StringRes
    private fun getHoroscopeMessageRes(percentage: Int): Int = when (percentage) {
        in 0..5 -> R.string.horoscope_message_0_5
        in 6..20 -> R.string.horoscope_message_6_20
        in 21..35 -> R.string.horoscope_message_21_35
        in 36..50 -> R.string.horoscope_message_36_50
        in 51..70 -> R.string.horoscope_message_51_70
        in 71..90 -> R.string.horoscope_message_71_90
        else -> R.string.horoscope_message_91_100
    }

    companion object {
        private const val EXTRA_PERSON_ONE = "com.example.lovetester.extra.PERSON_ONE"
        private const val EXTRA_PERSON_TWO = "com.example.lovetester.extra.PERSON_TWO"
        private const val EXTRA_PERCENTAGE = "com.example.lovetester.extra.PERCENTAGE"
        private const val EXTRA_SIGN_ONE = "com.example.lovetester.extra.SIGN_ONE"
        private const val EXTRA_SIGN_TWO = "com.example.lovetester.extra.SIGN_TWO"

        fun newNameTestIntent(
            context: Context,
            personOne: String,
            personTwo: String,
            percentage: Int
        ) = Intent(context, ResultActivity::class.java)
            .putExtra(EXTRA_PERSON_ONE, personOne)
            .putExtra(EXTRA_PERSON_TWO, personTwo)
            .putExtra(EXTRA_PERCENTAGE, percentage)

        fun newHoroscopeIntent(
            context: Context,
            mySign: ZodiacSign,
            partnerSign: ZodiacSign,
            percentage: Int
        ) = Intent(context, ResultActivity::class.java)
            .putExtra(EXTRA_SIGN_ONE, mySign.name)
            .putExtra(EXTRA_SIGN_TWO, partnerSign.name)
            .putExtra(EXTRA_PERCENTAGE, percentage)
    }
}