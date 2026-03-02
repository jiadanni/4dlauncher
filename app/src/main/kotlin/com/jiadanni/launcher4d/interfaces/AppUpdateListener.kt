package com.jiadanni.launcher4d.interfaces

import com.jiadanni.launcher4d.model.App

interface AppUpdateListener {
    fun onAppUpdated(apps: List<App>): Boolean
}
