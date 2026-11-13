package com.example.lovetester

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lovetester.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.setBackgroundResource(R.drawable.bg_splash)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, safeInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(binding.root)
        ViewCompat.requestApplyInsets(binding.root)
    }
}