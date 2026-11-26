package com.example.lovetester.ui.lovetest.name

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityNameTestBinding
import com.example.lovetester.ui.common.applySystemBarsAndImePadding
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import com.example.lovetester.ui.common.hideKeyboard
import com.example.lovetester.ui.common.onImeDone
import com.example.lovetester.ui.lovetest.domain.LoveTestCalculator
import com.example.lovetester.ui.lovetest.result.ResultActivity

class NameTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNameTestBinding
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()
        binding = ActivityNameTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsAndImePadding()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.etPartnerName.onImeDone()

        binding.btnLoveTest.setOnClickListener {
            if (isNavigating) return@setOnClickListener
            it.hideKeyboard()

            val yourName = binding.etYourName.text.toString().trim()
            val partnerName = binding.etPartnerName.text.toString().trim()

            if (yourName.isBlank()) {
                binding.etYourName.error = getString(R.string.name_test_error_empty_name)
                return@setOnClickListener
            }

            if (partnerName.isBlank()) {
                binding.etPartnerName.error = getString(R.string.name_test_error_empty_partner_name)
                return@setOnClickListener
            }

            val percentage = LoveTestCalculator.calculateNameTest(
                yourName,
                partnerName
            )

            isNavigating = true
            startActivity(
                ResultActivity.newNameTestIntent(
                    context = this,
                    personOne = yourName,
                    personTwo = partnerName,
                    percentage = percentage
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        isNavigating = false
    }
}