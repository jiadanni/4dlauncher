package com.jiadanni.launcher4d.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.feed.FeedCardAdapter
import com.jiadanni.launcher4d.manager.Setup

/**
 * Google Now-like feed view that displays cards with contextual information
 * such as weather, calendar events, travel time, and flight information.
 */
class FeedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView: RecyclerView
    private val feedAdapter: FeedCardAdapter

    init {
        LayoutInflater.from(context).inflate(R.layout.view_feed, this, true)

        recyclerView = findViewById(R.id.feed_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(false)

        feedAdapter = FeedCardAdapter(context)
        recyclerView.adapter = feedAdapter

        setBackgroundColor(Setup.appSettings().drawerBackgroundColor)
    }

    /**
     * Refresh all feed cards with latest data
     */
    fun refreshFeed() {
        feedAdapter.refreshCards()
    }

    /**
     * Show/hide the feed with animation
     */
    fun show(animate: Boolean = true) {
        if (animate) {
            animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .start()
        } else {
            alpha = 1f
            translationX = 0f
        }
    }

    /**
     * Hide the feed with animation
     */
    fun hide(animate: Boolean = true) {
        if (animate) {
            animate()
                .alpha(0f)
                .translationX(-width.toFloat())
                .setDuration(200)
                .start()
        } else {
            alpha = 0f
            translationX = -width.toFloat()
        }
    }

    /**
     * Called when feed becomes visible
     */
    fun onFeedVisible() {
        refreshFeed()
    }

    /**
     * Called when feed becomes hidden
     */
    fun onFeedHidden() {
        // Optionally pause updates when hidden
    }
}
