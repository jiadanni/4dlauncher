package com.benny.openlauncher.activity

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.benny.openlauncher.R
import com.benny.openlauncher.util.AppSettings
import kotlin.math.max

abstract class ColorActivity : AppCompatActivity() {

    protected lateinit var appSettings: AppSettings

    @Deprecated("Use appSettings property", ReplaceWith("appSettings"))
    protected val _appSettings: AppSettings
        get() = appSettings

    private var currentTheme: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appSettings = AppSettings.get()
        currentTheme = appSettings.theme

        when (appSettings.theme) {
            "0" -> setTheme(R.style.NormalActivity_Light)
            "1" -> setTheme(R.style.NormalActivity_Dark)
            else -> setTheme(R.style.NormalActivity_Black)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = dark(appSettings.primaryColor, 0.8)
            window.navigationBarColor = appSettings.primaryColor
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (appSettings.theme != currentTheme) {
            restart()
        }
    }

    protected fun restart() {
        val intent = Intent(this, javaClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        overridePendingTransition(0, 0)
        startActivity(intent)
    }

    fun dark(color: Int, factor: Double): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
            a,
            max((r * factor).toInt(), 0),
            max((g * factor).toInt(), 0),
            max((b * factor).toInt(), 0)
        )
    }
}
