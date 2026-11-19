package com.example.lovetester.ui.memory

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityMemoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.text.replace

@AndroidEntryPoint
class MemoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMemoryBinding

    private val viewModel: MemoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.memoryFragmentContainer,
                    MemoryListFragment()
                )
                .commit()
        }

    }
}