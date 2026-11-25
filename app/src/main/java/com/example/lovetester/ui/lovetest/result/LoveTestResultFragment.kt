
package com.example.lovetester.ui.lovetest.result

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLoveTestResultBinding
import com.example.lovetester.ui.lovetest.horoscope.ZodiacSign

class LoveTestResultFragment :
    Fragment(R.layout.fragment_love_test_result) {

    private var _binding: FragmentLoveTestResultBinding? = null

    private val binding: FragmentLoveTestResultBinding
        get() = requireNotNull(_binding)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoveTestResultBinding.bind(view)

        val args = requireArguments()

        val testType = args.getString(ARG_TEST_TYPE) ?: TEST_TYPE_NAME
        val isHoroscope = testType == TEST_TYPE_HOROSCOPE

        val percentage = args.getInt(ARG_PERCENTAGE)
        val (personOne, personTwo) = resolvePersonNames(args, isHoroscope)

        binding.tvPersonOne.text = personOne
        binding.tvPersonTwo.text = personTwo
        binding.tvPercentage.text = getString(R.string.percent_format, percentage)

        if (isHoroscope) bindZodiacIcons(args)

        val message = getString(
            if (isHoroscope) getHoroscopeMessageRes(percentage)
            else getNameTestMessageRes(percentage)
        )
        binding.tvResultMessage.text = message

        setupButtons(personOne, personTwo, percentage, message)
    }

    private fun resolvePersonNames(
        args: Bundle,
        isHoroscope: Boolean
    ): Pair<String, String> {
        if (isHoroscope) {
            val signOne = args.getString(ARG_SIGN_ONE)?.let { ZodiacSign.valueOf(it) }
            val signTwo = args.getString(ARG_SIGN_TWO)?.let { ZodiacSign.valueOf(it) }
            if (signOne != null && signTwo != null) {
                return getString(signOne.nameRes) to getString(signTwo.nameRes)
            }
        }

        return requireNotNull(args.getString(ARG_PERSON_ONE)) to
                requireNotNull(args.getString(ARG_PERSON_TWO))
    }

    private fun setupButtons(
        personOne: String,
        personTwo: String,
        percentage: Int,
        message: String
    ) {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRetry.setOnClickListener {
            findNavController().navigateUp()
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

    private fun bindZodiacIcons(args: Bundle) {
        args.getString(ARG_SIGN_ONE)?.let {
            binding.ivPersonOne.setImageResource(ZodiacSign.valueOf(it).iconRes)
        }
        args.getString(ARG_SIGN_TWO)?.let {
            binding.ivPersonTwo.setImageResource(ZodiacSign.valueOf(it).iconRes)
        }

        val pink = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.primary)
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
        const val ARG_PERSON_ONE = "personOne"
        const val ARG_PERSON_TWO = "personTwo"
        const val ARG_PERCENTAGE = "percentage"
        const val ARG_TEST_TYPE = "testType"
        const val ARG_SIGN_ONE = "signOne"
        const val ARG_SIGN_TWO = "signTwo"

        const val TEST_TYPE_NAME = "name"
        const val TEST_TYPE_HOROSCOPE = "horoscope"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
