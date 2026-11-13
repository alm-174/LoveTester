package com.example.lovetester.ui.intro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIntroBinding

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)


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

        // 2. Kết nối Adapter với ViewPager2
        val adapter = IntroPagerAdapter(pages)
        binding.viewPagerIntro.adapter = adapter

        // 3. Theo dõi khi trang thay đổi
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
            val currentPage = binding.viewPagerIntro.currentItem
            val lastPage = pages.lastIndex

            if (currentPage < lastPage) {
                binding.viewPagerIntro.setCurrentItem(
                    currentPage + 1,
                    true
                )
            } else {
                // TODO: Chuyển sang HomeFragment
                // findNavController().navigate(R.id.action_intro_to_home)

//                val intent = Intent(this, PermissionActivity::class.java)
//                startActivity(intent)
            }
        }

        // 5. Cập nhật dấu chấm ban đầu
        updateIndicators(
            binding,
            binding.viewPagerIntro.currentItem
        )
    }

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