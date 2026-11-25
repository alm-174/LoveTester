package com.example.lovetester.ui.lovetest

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lovetester.R
import com.example.lovetester.databinding.FragmentLoveTestBinding
import com.example.lovetester.ui.common.safeNavigate
import com.example.lovetester.ui.common.viewBinding

class LoveTestFragment : Fragment(R.layout.fragment_love_test) {

    private val binding by viewBinding(FragmentLoveTestBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupTitle()
        setupCardClicks()
    }

    /**
     * Colours the LAST word of the (translated) title with the primary colour.
     * A single-word title (e.g. German "Liebestest") stays one colour.
     */
    private fun setupTitle() {
        val title = getString(R.string.love_test_title)
        val accentStart = title.lastIndexOf(' ') + 1

        binding.tvLoveTestTitle.text = if (accentStart > 0) {
            buildSpannedString {
                append(title.substring(0, accentStart))
                color(ContextCompat.getColor(requireContext(), R.color.primary)) {
                    append(title.substring(accentStart))
                }
            }
        } else {
            title
        }
    }

    private fun setupCardClicks() {

        binding.cardNameTest.setOnClickListener {
            findNavController().safeNavigate(R.id.action_loveTest_to_nameTest)
        }

        binding.cardHoroscopeTest.setOnClickListener {
            findNavController().safeNavigate(R.id.action_loveTest_to_horoscopeTest)
        }
    }

}
