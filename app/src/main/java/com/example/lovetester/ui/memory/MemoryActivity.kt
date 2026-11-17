package com.example.lovetester.ui.memory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivityLoveTestBinding
import com.example.lovetester.databinding.ActivityMemoryBinding

class MemoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMemoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}