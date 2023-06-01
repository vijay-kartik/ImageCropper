package com.vkartik.imagecropper

import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

fun getScreenParams(windowManager: WindowManager): Array<Int> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())

        arrayOf(
            windowMetrics.bounds.width() - insets.left - insets.right,
            windowMetrics.bounds.height() - insets.top - insets.bottom
        )
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        arrayOf(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }