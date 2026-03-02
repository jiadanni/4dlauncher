package com.jiadanni.launcher4d.interfaces

import com.jiadanni.launcher4d.model.App

interface AppDeleteListener {
    fun onAppDeleted(apps: List<App>): Boolean
}
