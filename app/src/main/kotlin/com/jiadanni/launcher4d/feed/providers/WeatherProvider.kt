package com.jiadanni.launcher4d.feed.providers

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.jiadanni.launcher4d.feed.FeedCard
import com.jiadanni.launcher4d.util.AppSettings
import org.json.JSONObject
import java.net.URL

/**
 * Provides weather information for feed cards
 * Uses OpenWeatherMap API (free tier available)
 * Get your free API key at: https://openweathermap.org/api
 */
class WeatherProvider(private val context: Context) {

    private val baseUrl = "https://api.openweathermap.org/data/2.5/weather"

    private val apiKey: String
        get() = AppSettings.get().feedWeatherApiKey

    /**
     * Get current weather card
     */
    suspend fun getWeatherCard(): FeedCard.WeatherCard? {
        // Check if API key is configured
        if (apiKey.isBlank()) {
            return null // No API key configured, return null
        }

        return try {
            val location = getLastKnownLocation() ?: return null
            fetchWeatherData(location.latitude, location.longitude)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getLastKnownLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return try {
            locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            null
        }
    }

    private suspend fun fetchWeatherData(lat: Double, lon: Double): FeedCard.WeatherCard? {
        return try {
            val url = "$baseUrl?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
            val response = URL(url).readText()
            parseWeatherResponse(response)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseWeatherResponse(jsonString: String): FeedCard.WeatherCard {
        val json = JSONObject(jsonString)
        val main = json.getJSONObject("main")
        val weather = json.getJSONArray("weather").getJSONObject(0)

        return FeedCard.WeatherCard(
            location = json.getString("name"),
            temperature = main.getDouble("temp").toInt(),
            condition = weather.getString("description").capitalize(),
            iconCode = weather.getString("icon"),
            highTemp = main.getDouble("temp_max").toInt(),
            lowTemp = main.getDouble("temp_min").toInt(),
            humidity = main.getInt("humidity"),
            feelsLike = main.getDouble("feels_like").toInt()
        )
    }

    /**
     * Mock weather data for development/testing
     */
    private fun getMockWeatherCard(): FeedCard.WeatherCard {
        return FeedCard.WeatherCard(
            location = "Current Location",
            temperature = 72,
            condition = "Partly Cloudy",
            iconCode = "02d",
            highTemp = 78,
            lowTemp = 65,
            humidity = 60,
            feelsLike = 71
        )
    }
}
