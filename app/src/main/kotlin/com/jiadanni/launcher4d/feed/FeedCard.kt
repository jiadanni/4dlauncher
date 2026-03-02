package com.jiadanni.launcher4d.feed

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Base class for all feed cards
 */
sealed class FeedCard {
    abstract val priority: Int  // Higher priority cards appear first
    abstract val cardType: CardType

    enum class CardType {
        WEATHER,
        CALENDAR_EVENT,
        TRAVEL_TIME,
        FLIGHT_INFO,
        NEWS,
        REMINDER
    }

    /**
     * Weather card showing current conditions and forecast
     */
    data class WeatherCard(
        val location: String,
        val temperature: Int,
        val condition: String,
        val iconCode: String,
        val highTemp: Int,
        val lowTemp: Int,
        val humidity: Int,
        val feelsLike: Int,
        override val priority: Int = 100
    ) : FeedCard() {
        override val cardType = CardType.WEATHER
    }

    /**
     * Calendar event card
     */
    data class CalendarEventCard(
        val eventId: Long,
        val title: String,
        val startTime: Long,
        val endTime: Long,
        val location: String?,
        val description: String?,
        val allDay: Boolean,
        val color: Int,
        override val priority: Int = 90
    ) : FeedCard() {
        override val cardType = CardType.CALENDAR_EVENT
    }

    /**
     * Travel time card showing estimated time to a location
     */
    data class TravelTimeCard(
        val destination: String,
        val eventTitle: String,
        val eventStartTime: Long,
        val estimatedMinutes: Int,
        val trafficCondition: TrafficCondition,
        val departureTime: Long,  // Suggested departure time
        override val priority: Int = 85
    ) : FeedCard() {
        override val cardType = CardType.TRAVEL_TIME
    }

    enum class TrafficCondition {
        CLEAR, MODERATE, HEAVY
    }

    /**
     * Flight information card
     */
    data class FlightInfoCard(
        val flightNumber: String,
        val airline: String,
        val departure: String,
        val arrival: String,
        val departureTime: Long,
        val arrivalTime: Long,
        val gate: String?,
        val seat: String?,
        val confirmationCode: String?,
        val status: FlightStatus,
        override val priority: Int = 95
    ) : FeedCard() {
        override val cardType = CardType.FLIGHT_INFO
    }

    enum class FlightStatus {
        SCHEDULED, ON_TIME, DELAYED, BOARDING, IN_AIR, LANDED, ARRIVED, CANCELLED
    }

    /**
     * News/article card
     */
    data class NewsCard(
        val title: String,
        val source: String,
        val summary: String,
        val imageUrl: String?,
        val url: String,
        val publishTime: Long,
        override val priority: Int = 50
    ) : FeedCard() {
        override val cardType = CardType.NEWS
    }

    /**
     * Reminder card
     */
    data class ReminderCard(
        val reminderId: Long,
        val title: String,
        val description: String?,
        val dueTime: Long,
        val completed: Boolean,
        override val priority: Int = 80
    ) : FeedCard() {
        override val cardType = CardType.REMINDER
    }
}

/**
 * ViewHolder for feed cards
 */
abstract class FeedCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(card: FeedCard)
}
