package com.example.lovetester.ui.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController

fun NavController.safeNavigate(@IdRes actionId: Int, args: Bundle? = null) {
    if (currentDestination?.getAction(actionId) != null) {
        navigate(actionId, args)
    }
}