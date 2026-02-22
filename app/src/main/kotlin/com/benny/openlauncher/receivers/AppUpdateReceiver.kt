package com.benny.openlauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.benny.openlauncher.manager.Setup

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Setup.appLoader().onAppUpdated(context, intent)
    }
}
