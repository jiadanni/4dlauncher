package com.benny.openlauncher.interfaces

import com.benny.openlauncher.model.App

interface AppDeleteListener {
    fun onAppDeleted(apps: List<App>): Boolean
}
