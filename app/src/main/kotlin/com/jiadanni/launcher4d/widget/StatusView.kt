package com.jiadanni.launcher4d.widget

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets

class StatusView(context: Context, attr: AttributeSet) : View(context, attr) {

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // scale the view to pad the home layout for the missing status and navigation bars
        // TODO move home layout to class so this can be done within the view itself
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val inset = insets.systemWindowInsetTop
            if (inset != 0) {
                val layoutParams = getLayoutParams()
                layoutParams.height = inset
                setLayoutParams(layoutParams)
                visibility = VISIBLE
            }
        }
        return insets
    }
}
