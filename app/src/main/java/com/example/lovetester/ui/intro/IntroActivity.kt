package com.example.lovetester.ui.intro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.lovetester.R
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.databinding.ActivityIntroBinding
import com.example.lovetester.ui.language.LanguageActivity
import com.example.lovetester.ui.permission.PermissionActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding
    private lateinit var repository: LoveSettingsRepository

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LoveSettingsRepository(applicationContext)

        // 1. Danh sách 3 trang giới thiệu
        val pages = listOf(
            IntroPage(
                R.drawable.intro_relationship,
                R.string.intro_title_1,
                R.string.intro_description_1
            ),
            IntroPage(
                R.drawable.intro_day_counter,
                R.string.intro_title_2,
                R.string.intro_description_2
            ),
            IntroPage(
                R.drawable.intro_memories,
                R.string.intro_title_3,
                R.string.intro_description_3
            )
        )

        // 2. Kết nối ViewPager2 với Adapter
        val adapter = IntroPagerAdapter(pages)
        binding.viewPagerIntro.adapter = adapter

        // 3. Cập nhật dấu chấm khi trang thay đổi
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(binding, position)
            }
        }

        pageChangeCallback = callback
        binding.viewPagerIntro.registerOnPageChangeCallback(callback)

        // 4. Xử lý nút Next
        binding.btnNext.setOnClickListener {
            if (isNavigating) return@setOnClickListener

            val currentPage = binding.viewPagerIntro.currentItem
            val lastPage = pages.lastIndex

            if (currentPage < lastPage) {
                binding.viewPagerIntro.setCurrentItem(
                    currentPage + 1,
                    true
                )
            } else {
                navigateToPermission()
            }
        }

        // 5. Xử lý Back hệ thống
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleIntroBack()
                }
            }
        )

        // 6. Hiển thị dấu chấm ban đầu
        updateIndicators(
            binding,
            binding.viewPagerIntro.currentItem
        )
    }

    // Chỉ gọi khi hoàn thành trang Intro cuối
    private fun navigateToPermission() {
        if (isNavigating) return

        isNavigating = true
        binding.btnNext.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.setOnboardingStep(OnboardingStep.PERMISSION)

                val intent = Intent(
                    this@IntroActivity,
                    PermissionActivity::class.java
                )

                startActivity(intent)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isNavigating = false
                binding.btnNext.isEnabled = true

                Toast.makeText(
                    this@IntroActivity,
                    getString(R.string.intro_save_progress_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Xử lý Back trong Intro
    private fun handleIntroBack() {
        if (isNavigating) return

        val currentPage = binding.viewPagerIntro.currentItem

        // Nếu không phải trang đầu thì quay về trang trước
        if (currentPage > 0) {
            binding.viewPagerIntro.setCurrentItem(
                currentPage - 1,
                true
            )
            return
        }

        // Đang ở trang đầu: quay về Language
        navigateToLanguage()
    }

    private fun navigateToLanguage() {
        if (isNavigating) return

        isNavigating = true
        binding.btnNext.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.setOnboardingStep(OnboardingStep.LANGUAGE)

                val intent = Intent(
                    this@IntroActivity,
                    LanguageActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isNavigating = false
                binding.btnNext.isEnabled = true

                Toast.makeText(
                    this@IntroActivity,
                    getString(R.string.intro_back_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Cập nhật dấu chấm chỉ báo trang hiện tại
    private fun updateIndicators(
        binding: ActivityIntroBinding,
        selectedPosition: Int
    ) {
        val dots = listOf(
            binding.dot1,
            binding.dot2,
            binding.dot3
        )

        dots.forEachIndexed { index, dot ->
            val isSelected = index == selectedPosition

            dot.setBackgroundResource(
                if (isSelected) {
                    R.drawable.bg_intro_dot_active
                } else {
                    R.drawable.bg_intro_dot_inactive
                }
            )

            val params = dot.layoutParams

            params.width = if (isSelected) {
                dpToPx(20)
            } else {
                dpToPx(8)
            }

            dot.layoutParams = params
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (
                dp * resources.displayMetrics.density + 0.5f
                ).toInt()
    }

    override fun onDestroy() {
        pageChangeCallback?.let { callback ->
            binding.viewPagerIntro.unregisterOnPageChangeCallback(callback)
        }

        pageChangeCallback = null
        super.onDestroy()
    }
}