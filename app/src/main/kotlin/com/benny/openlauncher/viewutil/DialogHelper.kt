package com.benny.openlauncher.viewutil

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

object DialogHelper {
    fun alertDialog(context: Context, title: String, message: String, positiveBtn: String, positiveListener: DialogInterface.OnClickListener? = null) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveBtn, positiveListener)
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    fun alertDialog(context: Context, title: String, message: String, positiveListener: DialogInterface.OnClickListener? = null) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok, positiveListener)
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }
}
