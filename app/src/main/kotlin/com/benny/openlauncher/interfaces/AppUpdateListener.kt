package com.benny.openlauncher.interfaces

import com.benny.openlauncher.model.App

interface AppUpdateListener {
    fun onAppUpdated(apps: List<App>): Boolean
}
