package com.benny.openlauncher.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class AutoFinishActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AutoFinishActivity::class.java))
        }
    }
}
