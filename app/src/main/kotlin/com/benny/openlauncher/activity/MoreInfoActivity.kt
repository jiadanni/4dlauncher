package com.benny.openlauncher.activity

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.benny.openlauncher.R
import com.benny.openlauncher.fragment.SettingsAboutFragment

class MoreInfoActivity : ColorActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setBackgroundColor(appSettings.primaryColor)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val transaction = supportFragmentManager.beginTransaction()
        val settingsAboutFragment = SettingsAboutFragment.newInstance()
        transaction.replace(R.id.fragment_holder, settingsAboutFragment, SettingsAboutFragment.TAG).commit()
    }
}
