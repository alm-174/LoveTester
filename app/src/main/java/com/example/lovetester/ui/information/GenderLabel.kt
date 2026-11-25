package com.example.lovetester.ui.information

import androidx.annotation.StringRes
import com.example.lovetester.R
import com.example.lovetester.data.local.model.Gender

@StringRes
fun Gender.labelRes(): Int = when (this) {
    Gender.MALE -> R.string.common_gender_male
    Gender.FEMALE -> R.string.common_gender_female
    Gender.OTHER -> R.string.common_gender_other
}