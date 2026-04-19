package com.jiadanni.launcher4d.notifications

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.slf4j.LoggerFactory

class NotificationListener : NotificationListenerService() {

    private var isConnected = false
    private var notificationReceiver: NotificationListenerReceiver? = null

    @SuppressLint("HandlerLeak")
    private val monitorHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EVENT_UPDATE_CURRENT_NOS -> updateCurrentNotifications()
            }
        }
    }

    interface NotificationCallback {
        fun notificationCallback(count: Int)
    }

    override fun onCreate() {
        super.onCreate()

        if (notificationReceiver == null) {
            notificationReceiver = NotificationListenerReceiver()
            val filter = IntentFilter(UPDATE_NOTIFICATIONS_ACTION)
            registerReceiver(notificationReceiver, filter)
        }
    }

    override fun onDestroy() {
        notificationReceiver?.let { unregisterReceiver(it) }
        notificationReceiver = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        LOG.debug("Listener connected")
        isConnected = true

        monitorHandler.sendMessage(monitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Some apps do not track total notifications in their StatusBarNotification; given this
        // is an onNotificationPosted, ensure we have a minimum count to display the badge, otherwise
        // it will be removed which is counter-intuitive.
        var notificationCount = sbn.notification.number
        if (notificationCount == 0) {
            notificationCount = 1
        }
        processCallback(sbn.packageName, notificationCount)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        processCallback(sbn.packageName, 0)
    }

    private fun processCallback(packageName: String, count: Int) {
        LOG.debug("processCallback({}) -> {}", packageName, count)
        val callbacks = _currentNotifications[packageName]

        callbacks?.forEach { callback ->
            callback.notificationCallback(count)
        }
    }

    private fun updateCurrentNotifications() {
        if (isConnected) {
            try {
                val activeNos = activeNotifications

                var packageName = ""
                var notificationCount = 0
                for (i in activeNos.indices) {
                    val pkg = activeNos[i].packageName
                    if (packageName != pkg) {
                        packageName = pkg
                        notificationCount = 0
                    }
                    val count = activeNos[i].notification.number
                    notificationCount = if (count == 0) {
                        notificationCount + 1
                    } else {
                        maxOf(notificationCount, count)
                    }

                    processCallback(packageName, notificationCount)
                }
            } catch (e: Exception) {
                LOG.error("Unexpected exception when updating notifications: {}", e)
            }
        }
    }

    inner class NotificationListenerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UPDATE_NOTIFICATIONS_ACTION && intent.getStringExtra(UPDATE_NOTIFICATIONS_COMMAND) == UPDATE_NOTIFICATIONS_UPDATE) {
                this@NotificationListener.updateCurrentNotifications()
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("NotificationListener")

        const val UPDATE_NOTIFICATIONS_ACTION = "update-notifications"
        const val UPDATE_NOTIFICATIONS_COMMAND = "command"
        const val UPDATE_NOTIFICATIONS_UPDATE = "update"

        private const val EVENT_UPDATE_CURRENT_NOS = 0

        private val _currentNotifications = HashMap<String, ArrayList<NotificationCallback>>()

        @JvmStatic
        fun setNotificationCallback(packageName: String, callback: NotificationCallback) {
            var callbacks = _currentNotifications[packageName]

            if (callbacks != null) {
                callbacks.add(callback)
            } else {
                callbacks = ArrayList(1)
                callbacks.add(callback)
                _currentNotifications[packageName] = callbacks
            }
        }
    }
}
