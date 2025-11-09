package com.benny.openlauncher.feed.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.benny.openlauncher.feed.FeedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Provides travel time estimates to calendar event locations
 */
class TravelTimeProvider(private val context: Context) {

    private val calendarProvider = CalendarProvider(context)

    /**
     * Get travel time cards for upcoming calendar events with locations
     */
    suspend fun getTravelTimeCards(hoursAhead: Int = 6): List<FeedCard.TravelTimeCard> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext emptyList()
        }

        val currentLocation = getLastKnownLocation() ?: return@withContext emptyList()
        val upcomingEvents = calendarProvider.getUpcomingEvents(hoursAhead)

        val travelCards = mutableListOf<FeedCard.TravelTimeCard>()

        for (event in upcomingEvents) {
            // Only process events with a location
            val eventLocation = event.location
            if (eventLocation.isNullOrBlank()) continue

            // Skip if event is more than 3 hours away (too early to show travel time)
            val timeUntilEvent = event.startTime - System.currentTimeMillis()
            if (timeUntilEvent > 3 * 60 * 60 * 1000) continue

            // Skip if event already started
            if (timeUntilEvent < 0) continue

            try {
                val travelTime = estimateTravelTime(currentLocation, eventLocation)
                if (travelTime != null) {
                    travelCards.add(
                        FeedCard.TravelTimeCard(
                            destination = eventLocation,
                            eventTitle = event.title,
                            eventStartTime = event.startTime,
                            estimatedMinutes = travelTime.durationMinutes,
                            trafficCondition = travelTime.trafficCondition,
                            departureTime = calculateDepartureTime(event.startTime, travelTime.durationMinutes)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext travelCards
    }

    /**
     * Estimate travel time between current location and destination
     * Uses a simple distance-based estimation (in a real implementation, you'd use Google Maps API)
     */
    private suspend fun estimateTravelTime(from: Location, destination: String): TravelTimeEstimate? {
        // For demo purposes, we'll use a simple estimation
        // In production, you should use Google Maps Distance Matrix API

        // Parse destination coordinates if possible (simplified)
        // In reality, you'd geocode the address string

        // For now, return a mock estimation based on random factors
        val baseMinutes = (15..45).random()
        val traffic = FeedCard.TrafficCondition.MODERATE

        return TravelTimeEstimate(
            durationMinutes = baseMinutes,
            trafficCondition = traffic
        )
    }

    /**
     * Calculate when to leave based on event start time and travel duration
     */
    private fun calculateDepartureTime(eventStartTime: Long, travelMinutes: Int): Long {
        // Add 10 minute buffer
        return eventStartTime - ((travelMinutes + 10) * 60 * 1000)
    }

    /**
     * Get the last known location
     */
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private data class TravelTimeEstimate(
        val durationMinutes: Int,
        val trafficCondition: FeedCard.TrafficCondition
    )
}
