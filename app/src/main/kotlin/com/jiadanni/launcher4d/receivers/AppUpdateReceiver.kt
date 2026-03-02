package com.jiadanni.launcher4d.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiadanni.launcher4d.manager.Setup

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Setup.appLoader().onAppUpdated(context, intent)
    }
}
