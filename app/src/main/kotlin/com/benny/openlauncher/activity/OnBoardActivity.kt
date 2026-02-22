package com.benny.openlauncher.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.benny.openlauncher.R

class OnBoardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Skip onboarding as the library is missing/deprecated
        setState()
        finish()
    }

    private fun setState() {
        getSharedPreferences("app", Context.MODE_PRIVATE).edit()
            .putBoolean(resources.getString(R.string.pref_key__show_intro), false)
            .apply()

        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }
}
