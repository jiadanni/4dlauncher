package com.benny.openlauncher

import android.app.Application

class AppObject : Application() {
    override fun onCreate() {
        super.onCreate()
        _instance = this
    }

    companion object {
        private var _instance: AppObject? = null

        fun get(): AppObject = _instance ?: throw IllegalStateException("App not initialized")
    }
}
