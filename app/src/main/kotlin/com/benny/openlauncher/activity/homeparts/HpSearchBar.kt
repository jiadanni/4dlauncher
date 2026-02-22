package com.benny.openlauncher.activity.homeparts

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.view.View
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.widget.SearchBar
import net.gsantner.opoc.util.ActivityUtils

class HpSearchBar(
    private val homeActivity: HomeActivity,
    private val searchBar: SearchBar
) : SearchBar.CallBack, View.OnClickListener {

    fun initSearchBar() {
        searchBar.setCallback(this)
        searchBar._searchClock.setOnClickListener(this)
        homeActivity.updateSearchClock()
    }

    override fun onInternetSearch(string: String) {
        val intent = Intent()

        if (Tool.isIntentActionAvailable(homeActivity.applicationContext, Intent.ACTION_WEB_SEARCH)
            && !Setup.appSettings().searchBarForceBrowser) {
            intent.action = Intent.ACTION_WEB_SEARCH
            intent.putExtra(SearchManager.QUERY, string)
        } else {
            val baseUri = Setup.appSettings().searchBarBaseURI
            val searchUri = if (baseUri.contains("{query}")) {
                baseUri.replace("{query}", string)
            } else {
                baseUri + string
            }

            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(searchUri)
        }

        try {
            homeActivity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onExpand() {
        searchBar._searchInput.isFocusable = true
        searchBar._searchInput.isFocusableInTouchMode = true
        searchBar._searchInput.postDelayed({
            homeActivity.dimBackground()
            homeActivity.clearRoomForPopUp()
            searchBar._searchInput.requestFocus()
        }, 100)
        Tool.showKeyboard(homeActivity, searchBar._searchInput)
    }

    override fun onCollapse() {
        homeActivity.desktop.postDelayed({
            homeActivity.unDimBackground()
            homeActivity.unClearRoomForPopUp()
            searchBar._searchInput.clearFocus()
        }, 100)
        Tool.hideKeyboard(homeActivity, searchBar._searchInput)
    }

    override fun onClick(v: View) {
        ActivityUtils(homeActivity).startCalendarApp().freeContextRef()
    }
}
