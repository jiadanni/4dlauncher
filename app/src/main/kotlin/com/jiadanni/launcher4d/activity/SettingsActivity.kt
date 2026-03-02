package com.jiadanni.launcher4d.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.fragment.SettingsBaseFragment
import com.jiadanni.launcher4d.fragment.SettingsMasterFragment
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.util.BackupHelper
import com.jiadanni.launcher4d.util.Definitions
import com.nononsenseapps.filepicker.Utils
import net.gsantner.opoc.util.ContextUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class SettingsActivity : ColorActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var toolbar: Toolbar

    public override fun onCreate(b: Bundle?) {
        // must be applied before setContentView
        super.onCreate(b)
        val contextUtils = ContextUtils(this)
        contextUtils.setAppLanguage(appSettings.language)

        setContentView(R.layout.activity_settings)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setTitle(R.string.pref_title__settings)
        setSupportActionBar(toolbar)
        toolbar.navigationIcon = resources.getDrawable(R.drawable.ic_arrow_back_white, null)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        toolbar.setBackgroundColor(appSettings.primaryColor)

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_holder, SettingsMasterFragment()).commit()

        // if system exit is called the app will open settings activity again
        // this pushes the user back out to the home activity
        if (appSettings.appRestartRequired) {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, preference: Preference): Boolean {
        val fragment = Fragment.instantiate(this, preference.fragment!!, preference.extras)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_holder, fragment).addToBackStack(fragment.tag).commit()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            Setup.dataManager().close()
            val files = Utils.getSelectedFilesFromResult(data)
            when (requestCode) {
                Definitions.INTENT_BACKUP -> {
                    val timestamp = SimpleDateFormat("yyyyMMdd'T'HHmmss").format(Date())
                    val file = File(Utils.getFileForUri(files[0]).absolutePath + "/openlauncher_$timestamp.zip")
                    BackupHelper.backupConfig(this, file.toString())
                    Setup.dataManager().open()
                }
                Definitions.INTENT_RESTORE -> {
                    BackupHelper.restoreConfig(this, Utils.getFileForUri(files[0]).toString())
                    System.exit(0)
                }
            }
        }
    }
}
