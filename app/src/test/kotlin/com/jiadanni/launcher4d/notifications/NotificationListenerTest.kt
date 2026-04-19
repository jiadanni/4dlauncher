package com.jiadanni.launcher4d.notifications

import android.content.Context
import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NotificationListenerTest {

    @Test
    fun `NotificationListenerReceiver onReceive should update current notifications when action and command match`() {
        val listener = spy(NotificationListener())
        val receiver = listener.NotificationListenerReceiver()
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)

        `when`(intent.action).thenReturn(NotificationListener.UPDATE_NOTIFICATIONS_ACTION)
        `when`(intent.getStringExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND))
            .thenReturn(NotificationListener.UPDATE_NOTIFICATIONS_UPDATE)

        // Mocking behavior to avoid calling real updateCurrentNotifications which might fail in test environment
        doNothing().`when`(listener).updateCurrentNotifications()

        receiver.onReceive(context, intent)

        verify(listener).updateCurrentNotifications()
    }

    @Test
    fun `NotificationListenerReceiver onReceive should NOT update current notifications when action does NOT match`() {
        val listener = spy(NotificationListener())
        val receiver = listener.NotificationListenerReceiver()
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)

        `when`(intent.action).thenReturn("WRONG_ACTION")
        `when`(intent.getStringExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND))
            .thenReturn(NotificationListener.UPDATE_NOTIFICATIONS_UPDATE)

        receiver.onReceive(context, intent)

        verify(listener, never()).updateCurrentNotifications()
    }

    @Test
    fun `NotificationListenerReceiver onReceive should NOT update current notifications when command does NOT match`() {
        val listener = spy(NotificationListener())
        val receiver = listener.NotificationListenerReceiver()
        val context = mock(Context::class.java)
        val intent = mock(Intent::class.java)

        `when`(intent.action).thenReturn(NotificationListener.UPDATE_NOTIFICATIONS_ACTION)
        `when`(intent.getStringExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND))
            .thenReturn("WRONG_COMMAND")

        receiver.onReceive(context, intent)

        verify(listener, never()).updateCurrentNotifications()
    }
}
