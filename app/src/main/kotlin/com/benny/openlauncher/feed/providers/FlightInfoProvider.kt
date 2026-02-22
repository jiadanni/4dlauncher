package com.benny.openlauncher.feed.providers

import android.content.Context
import com.benny.openlauncher.feed.FeedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Provides flight information by parsing calendar events
 */
class FlightInfoProvider(private val context: Context) {

    private val calendarProvider = CalendarProvider(context)

    // Regex patterns for flight information
    private val flightNumberPattern = Pattern.compile("([A-Z]{2}\\s*\\d{1,4})", Pattern.CASE_INSENSITIVE)
    private val confirmationPattern = Pattern.compile("(?:confirmation|booking|reference)\\s*:?\\s*([A-Z0-9]{6})", Pattern.CASE_INSENSITIVE)
    private val gatePattern = Pattern.compile("(?:gate|boarding)\\s*:?\\s*([A-Z]?\\d{1,3})", Pattern.CASE_INSENSITIVE)
    private val seatPattern = Pattern.compile("(?:seat)\\s*:?\\s*(\\d{1,2}[A-F])", Pattern.CASE_INSENSITIVE)

    /**
     * Get flight information cards from calendar events
     */
    suspend fun getFlightCards(hoursAhead: Int = 48): List<FeedCard.FlightInfoCard> = withContext(Dispatchers.IO) {
        val upcomingEvents = calendarProvider.getUpcomingEvents(hoursAhead)
        val flightCards = mutableListOf<FeedCard.FlightInfoCard>()

        for (event in upcomingEvents) {
            // Check if this looks like a flight event
            if (!calendarProvider.isFlightEvent(event)) continue

            // Try to extract flight information
            val flightInfo = parseFlightInfo(event)
            if (flightInfo != null) {
                flightCards.add(flightInfo)
            }
        }

        return@withContext flightCards
    }

    /**
     * Parse flight information from a calendar event
     */
    private fun parseFlightInfo(event: FeedCard.CalendarEventCard): FeedCard.FlightInfoCard? {
        val searchText = "${event.title} ${event.description ?: ""}"

        // Extract flight number
        val flightNumber = extractFlightNumber(searchText) ?: return null

        // Extract airline from flight number prefix
        val airline = extractAirline(flightNumber)

        // Extract other details
        val confirmationCode = extractConfirmationCode(searchText)
        val gate = extractGate(searchText)
        val seat = extractSeat(searchText)

        // Try to determine departure and arrival from location and title
        val (departure, arrival) = extractAirports(event.title, event.location)

        // Determine flight status based on time
        val status = determineFlight Status(event.startTime)

        return FeedCard.FlightInfoCard(
            flightNumber = flightNumber,
            airline = airline,
            departure = departure,
            arrival = arrival,
            departureTime = event.startTime,
            arrivalTime = event.endTime,
            gate = gate,
            seat = seat,
            confirmationCode = confirmationCode,
            status = status
        )
    }

    /**
     * Extract flight number from text
     */
    private fun extractFlightNumber(text: String): String? {
        val matcher = flightNumberPattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)?.replace("\\s+".toRegex(), "")
        } else {
            null
        }
    }

    /**
     * Extract airline name from flight number prefix
     */
    private fun extractAirline(flightNumber: String): String {
        val airlineCode = flightNumber.substring(0, 2)

        // Map common airline codes to names
        return when (airlineCode.uppercase()) {
            "AA" -> "American Airlines"
            "UA" -> "United Airlines"
            "DL" -> "Delta Air Lines"
            "WN" -> "Southwest Airlines"
            "B6" -> "JetBlue Airways"
            "AS" -> "Alaska Airlines"
            "NK" -> "Spirit Airlines"
            "F9" -> "Frontier Airlines"
            "BA" -> "British Airways"
            "LH" -> "Lufthansa"
            "AF" -> "Air France"
            "KL" -> "KLM"
            "EK" -> "Emirates"
            "QF" -> "Qantas"
            "SQ" -> "Singapore Airlines"
            "CX" -> "Cathay Pacific"
            "NH" -> "ANA"
            "JL" -> "Japan Airlines"
            else -> airlineCode.uppercase()
        }
    }

    /**
     * Extract confirmation/booking code
     */
    private fun extractConfirmationCode(text: String): String? {
        val matcher = confirmationPattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    /**
     * Extract gate information
     */
    private fun extractGate(text: String): String? {
        val matcher = gatePattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    /**
     * Extract seat information
     */
    private fun extractSeat(text: String): String? {
        val matcher = seatPattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    /**
     * Extract departure and arrival airports from title/location
     */
    private fun extractAirports(title: String, location: String?): Pair<String, String> {
        // Look for common patterns like "NYC to LAX", "JFK - SFO", etc.
        val arrowPattern = Pattern.compile("([A-Z]{3})\\s*(?:to|->|→)\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE)
        val dashPattern = Pattern.compile("([A-Z]{3})\\s*-\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE)

        val searchText = "$title ${location ?: ""}"

        var matcher = arrowPattern.matcher(searchText)
        if (matcher.find()) {
            return Pair(matcher.group(1) ?: "???", matcher.group(2) ?: "???")
        }

        matcher = dashPattern.matcher(searchText)
        if (matcher.find()) {
            return Pair(matcher.group(1) ?: "???", matcher.group(2) ?: "???")
        }

        // If we can't parse it, return generic values
        return Pair("Departure", "Arrival")
    }

    /**
     * Determine flight status based on current time
     */
    private fun determineFlightStatus(departureTime: Long): FeedCard.FlightStatus {
        val now = System.currentTimeMillis()
        val timeUntilDeparture = departureTime - now

        return when {
            timeUntilDeparture < -2 * 60 * 60 * 1000 -> FeedCard.FlightStatus.LANDED // More than 2 hours past
            timeUntilDeparture < 0 -> FeedCard.FlightStatus.IN_AIR // Departed but not landed yet
            timeUntilDeparture < 45 * 60 * 1000 -> FeedCard.FlightStatus.BOARDING // Less than 45 min
            timeUntilDeparture < 2 * 60 * 60 * 1000 -> FeedCard.FlightStatus.ON_TIME // Less than 2 hours
            else -> FeedCard.FlightStatus.SCHEDULED // More than 2 hours away
        }
    }
}
