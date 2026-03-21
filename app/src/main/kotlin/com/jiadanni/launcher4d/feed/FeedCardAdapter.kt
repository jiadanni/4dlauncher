package com.jiadanni.launcher4d.feed

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.feed.providers.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying feed cards
 */
class FeedCardAdapter(private val context: Context) : RecyclerView.Adapter<FeedCardViewHolder>() {

    private val cards = mutableListOf<FeedCard>()
    private val scope = CoroutineScope(Dispatchers.Main)

    // Data providers
    private val weatherProvider = WeatherProvider(context)
    private val calendarProvider = CalendarProvider(context)
    private val travelTimeProvider = TravelTimeProvider(context)
    private val flightInfoProvider = FlightInfoProvider(context)

    init {
        refreshCards()
    }

    override fun getItemCount(): Int = cards.size

    override fun getItemViewType(position: Int): Int {
        return when (cards[position].cardType) {
            FeedCard.CardType.WEATHER -> VIEW_TYPE_WEATHER
            FeedCard.CardType.CALENDAR_EVENT -> VIEW_TYPE_CALENDAR
            FeedCard.CardType.TRAVEL_TIME -> VIEW_TYPE_TRAVEL
            FeedCard.CardType.FLIGHT_INFO -> VIEW_TYPE_FLIGHT
            FeedCard.CardType.NEWS -> VIEW_TYPE_NEWS
            FeedCard.CardType.REMINDER -> VIEW_TYPE_REMINDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedCardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_WEATHER -> WeatherCardViewHolder(
                inflater.inflate(R.layout.card_feed_weather, parent, false)
            )
            VIEW_TYPE_CALENDAR -> CalendarCardViewHolder(
                inflater.inflate(R.layout.card_feed_calendar, parent, false)
            )
            VIEW_TYPE_TRAVEL -> TravelCardViewHolder(
                inflater.inflate(R.layout.card_feed_travel, parent, false)
            )
            VIEW_TYPE_FLIGHT -> FlightCardViewHolder(
                inflater.inflate(R.layout.card_feed_flight, parent, false)
            )
            VIEW_TYPE_NEWS -> NewsCardViewHolder(
                inflater.inflate(R.layout.card_feed_news, parent, false)
            )
            VIEW_TYPE_REMINDER -> ReminderCardViewHolder(
                inflater.inflate(R.layout.card_feed_reminder, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: FeedCardViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    /**
     * Refresh all feed cards from data providers
     */
    fun refreshCards() {
        scope.launch {
            val newCards = withContext(Dispatchers.IO) {
                val allCards = mutableListOf<FeedCard>()

                // Get weather card
                weatherProvider.getWeatherCard()?.let { allCards.add(it) }

                // Get calendar events for next 24 hours
                allCards.addAll(calendarProvider.getUpcomingEvents(24))

                // Get travel time cards
                allCards.addAll(travelTimeProvider.getTravelTimeCards())

                // Get flight info cards
                allCards.addAll(flightInfoProvider.getFlightCards())

                // Sort by priority (highest first)
                allCards.sortedByDescending { it.priority }
            }

            cards.clear()
            cards.addAll(newCards)
            notifyDataSetChanged()
        }
    }

    // ViewHolders for different card types

    class WeatherCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val locationText: TextView = itemView.findViewById(R.id.weather_location)
        private val temperatureText: TextView = itemView.findViewById(R.id.weather_temperature)
        private val conditionText: TextView = itemView.findViewById(R.id.weather_condition)
        private val highLowText: TextView = itemView.findViewById(R.id.weather_high_low)
        private val weatherIcon: ImageView = itemView.findViewById(R.id.weather_icon)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.WeatherCard) {
                locationText.text = card.location
                temperatureText.text = "${card.temperature}°"
                conditionText.text = card.condition
                highLowText.text = "H: ${card.highTemp}° L: ${card.lowTemp}°"
                // TODO: Set weather icon based on iconCode
            }
        }
    }

    class CalendarCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.calendar_title)
        private val timeText: TextView = itemView.findViewById(R.id.calendar_time)
        private val locationText: TextView = itemView.findViewById(R.id.calendar_location)
        private val colorIndicator: View = itemView.findViewById(R.id.calendar_color_indicator)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.CalendarEventCard) {
                titleText.text = card.title

                val timeFormat = android.text.format.DateFormat.getTimeFormat(itemView.context)
                val startDate = Date(card.startTime)
                val endDate = Date(card.endTime)
                timeText.text = if (card.allDay) {
                    "All day"
                } else {
                    "${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}"
                }

                card.location?.let {
                    locationText.visibility = View.VISIBLE
                    locationText.text = it
                } ?: run {
                    locationText.visibility = View.GONE
                }

                colorIndicator.setBackgroundColor(card.color)
            }
        }
    }

    class TravelCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val destinationText: TextView = itemView.findViewById(R.id.travel_destination)
        private val timeText: TextView = itemView.findViewById(R.id.travel_time)
        private val trafficText: TextView = itemView.findViewById(R.id.travel_traffic)
        private val modeIcon: ImageView = itemView.findViewById(R.id.travel_mode_icon)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.TravelTimeCard) {
                destinationText.text = card.eventTitle
                timeText.text = "${card.estimatedMinutes} min"

                trafficText.text = when (card.trafficCondition) {
                    FeedCard.TrafficCondition.CLEAR -> "Clear traffic"
                    FeedCard.TrafficCondition.MODERATE -> "Moderate traffic"
                    FeedCard.TrafficCondition.HEAVY -> "Heavy traffic"
                }
            }
        }
    }

    class FlightCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val flightNumberText: TextView = itemView.findViewById(R.id.flight_number)
        private val routeText: TextView = itemView.findViewById(R.id.flight_route)
        private val timeText: TextView = itemView.findViewById(R.id.flight_time)
        private val statusText: TextView = itemView.findViewById(R.id.flight_status)
        private val gateText: TextView = itemView.findViewById(R.id.flight_gate)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.FlightInfoCard) {
                flightNumberText.text = "${card.airline} ${card.flightNumber}"
                routeText.text = "${card.departure} → ${card.arrival}"

                val timeFormat = android.text.format.DateFormat.getTimeFormat(itemView.context)
                val departDate = Date(card.departureTime)
                timeText.text = "Departs ${timeFormat.format(departDate)}"

                statusText.text = when (card.status) {
                    FeedCard.FlightStatus.SCHEDULED -> "Scheduled"
                    FeedCard.FlightStatus.ON_TIME -> "On time"
                    FeedCard.FlightStatus.DELAYED -> "Delayed"
                    FeedCard.FlightStatus.BOARDING -> "Now boarding"
                    FeedCard.FlightStatus.IN_AIR -> "In flight"
                    FeedCard.FlightStatus.LANDED -> "Landed"
                    FeedCard.FlightStatus.ARRIVED -> "Arrived"
                    FeedCard.FlightStatus.CANCELLED -> "Cancelled"
                }

                card.gate?.let {
                    gateText.visibility = View.VISIBLE
                    gateText.text = "Gate $it"
                } ?: run {
                    gateText.visibility = View.GONE
                }
            }
        }
    }

    class NewsCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.news_title)
        private val sourceText: TextView = itemView.findViewById(R.id.news_source)
        private val summaryText: TextView = itemView.findViewById(R.id.news_summary)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.NewsCard) {
                titleText.text = card.title
                sourceText.text = card.source
                summaryText.text = card.summary
            }
        }
    }

    class ReminderCardViewHolder(itemView: View) : FeedCardViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.reminder_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.reminder_description)
        private val timeText: TextView = itemView.findViewById(R.id.reminder_time)

        override fun bind(card: FeedCard) {
            if (card is FeedCard.ReminderCard) {
                titleText.text = card.title
                card.description?.let {
                    descriptionText.visibility = View.VISIBLE
                    descriptionText.text = it
                } ?: run {
                    descriptionText.visibility = View.GONE
                }
                val timeFormat = android.text.format.DateFormat.getTimeFormat(itemView.context)
                timeText.text = timeFormat.format(Date(card.dueTime))
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_WEATHER = 1
        private const val VIEW_TYPE_CALENDAR = 2
        private const val VIEW_TYPE_TRAVEL = 3
        private const val VIEW_TYPE_FLIGHT = 4
        private const val VIEW_TYPE_NEWS = 5
        private const val VIEW_TYPE_REMINDER = 6
    }
}
