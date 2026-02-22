package com.benny.openlauncher.feed.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.benny.openlauncher.feed.FeedCard
import java.util.*

/**
 * Provides calendar event information from Android's Calendar provider
 */
class CalendarProvider(private val context: Context) {

    /**
     * Get upcoming calendar events for the next X hours
     */
    fun getUpcomingEvents(hoursAhead: Int = 24): List<FeedCard.CalendarEventCard> {
        if (!hasCalendarPermission()) {
            return emptyList()
        }

        val events = mutableListOf<FeedCard.CalendarEventCard>()
        val now = System.currentTimeMillis()
        val endTime = now + (hoursAhead * 60 * 60 * 1000)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_COLOR
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), endTime.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIndex = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val locationIndex = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val descIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val colorIndex = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_COLOR)

                while (cursor.moveToNext() && events.size < 5) { // Limit to 5 events
                    events.add(
                        FeedCard.CalendarEventCard(
                            eventId = cursor.getLong(idIndex),
                            title = cursor.getString(titleIndex) ?: "Untitled Event",
                            startTime = cursor.getLong(startIndex),
                            endTime = cursor.getLong(endIndex),
                            location = cursor.getString(locationIndex),
                            description = cursor.getString(descIndex),
                            allDay = cursor.getInt(allDayIndex) == 1,
                            color = cursor.getInt(colorIndex)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }

    /**
     * Check if calendar permission is granted
     */
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if an event might be a flight (based on keywords)
     */
    fun isFlightEvent(event: FeedCard.CalendarEventCard): Boolean {
        val flightKeywords = listOf("flight", "airline", "boarding", "departure", "arrival", "gate")
        val searchText = "${event.title} ${event.description}".lowercase()
        return flightKeywords.any { searchText.contains(it) }
    }
}
