
package com.example.lovetester.ui.lovetest.result

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLoveTestResultBinding

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

        val personOne = requireNotNull(
            args.getString("personOne")
        )

        val personTwo = requireNotNull(
            args.getString("personTwo")
        )

        val percentage = args.getInt("percentage")

        binding.tvPersonOne.text = personOne
        binding.tvPersonTwo.text = personTwo
        binding.tvPercentage.text = "$percentage%"

        binding.tvResultMessage.text =
            getNameTestMessage(percentage)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }


    private fun getNameTestMessage(percentage: Int): String {
        return when (percentage) {
            in 0..20 -> {
                "You should be friends not fall in love! " +
                        "It will either be the best relationship of " +
                        "your life, or you will lose that person " +
                        "forever. Sometimes, the greatest love " +
                        "stories are the ones that remain " +
                        "unspoken between best friends"
            }

            in 21..50 -> {
                "More than friends, we should be our " +
                        "loves! Love story should be started " +
                        "writing now! Love is like the wind, " +
                        "you can’t see it but you can feel it."
            }

            in 51..80 -> {
                "I want to hold you close to me and feel " +
                        "our hearts beat as one forever! You call it " +
                        "madness, but I call it love. We can only " +
                        "learn to love by loving"
            }

            in 81..100 -> {
                "I was born to be your lover! You are the " +
                        "apple of my eyes! We must be our own " +
                        "before we can be another’s. My soul and " +
                        "your soul are forever tangled"
            }

            else -> ""
        }
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
