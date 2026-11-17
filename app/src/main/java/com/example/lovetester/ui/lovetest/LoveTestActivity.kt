package com.example.lovetester.ui.lovetest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lovetester.databinding.ActivityHomeBinding
import com.example.lovetester.databinding.ActivityLoveTestBinding

class LoveTestActivity : AppCompatActivity(){
    private lateinit var binding: ActivityLoveTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoveTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}