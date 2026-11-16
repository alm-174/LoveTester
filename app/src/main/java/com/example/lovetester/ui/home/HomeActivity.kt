package com.example.lovetester.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivityHomeBinding
import com.example.lovetester.databinding.ActivityIntroBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}