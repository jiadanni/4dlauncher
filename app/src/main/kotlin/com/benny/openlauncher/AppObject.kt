package com.benny.openlauncher

import android.app.Application

class AppObject : Application() {
    override fun onCreate() {
        super.onCreate()
        _instance = this
    }

    companion object {
        var _instance: AppObject? = null
            private set
    }
}