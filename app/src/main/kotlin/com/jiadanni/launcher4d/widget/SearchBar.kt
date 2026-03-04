package com.jiadanni.launcher4d.widget

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.interfaces.AppUpdateListener
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.CircleDrawable
import com.jiadanni.launcher4d.viewutil.IconLabelItem
import com.mikepenz.fastadapter.IItemAdapter
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.IItem
import org.slf4j.LoggerFactory
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.Normalizer
import java.util.Locale

class SearchBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        @JvmStatic
        private val LOG = LoggerFactory.getLogger("SearchBar")
        private const val ANIM_TIME = 200L
    }

    lateinit var _searchClock: TextView
    lateinit var _switchButton: AppCompatImageView
    lateinit var _searchButton: AppCompatImageView
    lateinit var _searchInput: AppCompatEditText
    lateinit var _searchRecycler: RecyclerView

    private lateinit var _icon: CircleDrawable
    private lateinit var _searchCardContainer: CardView
    private val _itemAdapter = ItemAdapter<IconLabelItem>()
    private val _adapter = FastAdapter.with(_itemAdapter)
    private var _callback: CallBack? = null
    private var _expanded = false
    private val _searchClockTextSize = 28
    private val _searchClockSubTextFactor = 0.5f
    private var bottomInset = 0

    private val _clockModes = HashMap<Int, DateTimeFormatter>(4)
    private var _clockFormatterIndex = -1
    private var _clockFormatter: DateTimeFormatter? = null

    init {
        init()
    }

    fun setCallback(callback: CallBack?) {
        _callback = callback
    }

    fun collapse(): Boolean {
        if (!_expanded) {
            return false
        }
        _searchButton.callOnClick()
        return !_expanded
    }

    private fun init() {
        val dp1 = Tool.dp2px(1)
        val iconMarginOutside = dp1 * 16
        val iconMarginTop = dp1 * 14
        val searchTextMarginTop = dp1 * 4
        val iconSize = dp1 * 24
        val iconPadding = dp1 * 6

        // These have to match the Preferences Array, but without item 0 as that is a custom option which can be changed:
        //   <item>@string/custom</item>
        //   <item>February 17\nSaturday, 2018</item>
        //   <item>February 17\n15:48</item>
        //   <item>February 17, 2018\n15:48</item>
        //   <item>15:48\nFebruary 17, 2018</item>
        _clockModes[1] = DateTimeFormatter.ofPattern("MMMM dd\nEEEE, yyyy", Locale.getDefault())
        _clockModes[2] = DateTimeFormatter.ofPattern("MMMM dd\nHH:mm", Locale.getDefault())
        _clockModes[3] = DateTimeFormatter.ofPattern("MMMM dd, yyyy\nHH:mm", Locale.getDefault())
        _clockModes[4] = DateTimeFormatter.ofPattern("HH:mm\nMMMM dd, yyyy", Locale.getDefault())

        _searchClock = LayoutInflater.from(context).inflate(R.layout.view_search_clock, this, false) as TextView
        _searchClock.setTextSize(TypedValue.COMPLEX_UNIT_DIP, _searchClockTextSize.toFloat())
        val clockParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(iconMarginOutside, dp1 * 4, 0, dp1 * 4)
            gravity = Gravity.START
        }

        _switchButton = AppCompatImageView(context)
        _switchButton.setOnClickListener {
            Setup.appSettings().setSearchUseGrid(!Setup.appSettings().getSearchUseGrid())
            updateSwitchIcon()
            updateRecyclerViewLayoutManager()
        }
        _switchButton.visibility = View.GONE
        _switchButton.setPadding(0, iconPadding, 0, iconPadding)
        updateSwitchIcon()

        val switchButtonParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(iconMarginOutside / 2, 0, 0, 0)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        if (isInEditMode) return

        _icon = CircleDrawable(context, resources.getDrawable(R.drawable.ic_search), Color.WHITE, Color.BLACK, 100)
        _searchButton = AppCompatImageView(context)
        _searchButton.setImageDrawable(_icon)
        _searchButton.setOnClickListener {
            if (_expanded && (_searchInput.text?.length ?: 0) > 0) {
                _searchInput.text?.clear()
                return@setOnClickListener
            }
            _expanded = !_expanded
            if (_expanded) {
                expandInternal()
            } else {
                collapseInternal()
            }
        }

        val buttonParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, iconMarginTop, iconMarginOutside, 0)
            gravity = Gravity.END
        }

        _searchCardContainer = CardView(context)
        _searchCardContainer.setCardBackgroundColor(Color.TRANSPARENT)
        _searchCardContainer.visibility = View.GONE
        _searchCardContainer.radius = 0f
        _searchCardContainer.cardElevation = 0f
        _searchCardContainer.setContentPadding(dp1 * 4, dp1 * 4, dp1 * 4, dp1 * 4)

        _searchInput = AppCompatEditText(context)
        _searchInput.background = null
        _searchInput.setHint(R.string.search_hint)
        _searchInput.setHintTextColor(Color.WHITE)
        _searchInput.setTextColor(Color.WHITE)
        _searchInput.setSingleLine()
        _searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                _itemAdapter.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        _searchInput.setOnKeyListener { _, keyCode, event ->
            if (event != null && event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                _callback?.onInternetSearch(_searchInput.text.toString())
                _searchInput.text?.clear()
                true
            } else {
                false
            }
        }

        val inputCardParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, searchTextMarginTop, 0, 0)
        }

        val inputParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(iconMarginOutside + iconSize, 0, 0, 0)
        }

        _searchCardContainer.addView(_switchButton, switchButtonParams)
        _searchCardContainer.addView(_searchInput, inputParams)

        initRecyclerView()

        Setup.appLoader().addUpdateListener(object : AppUpdateListener {
            override fun onAppUpdated(apps: List<App>): Boolean {
                _itemAdapter.clear()
                var appList = apps
                if (Setup.appSettings().getSearchBarShouldShowHiddenApps()) {
                    appList = Setup.appLoader().getAllApps(context, true)
                }
                val items = ArrayList<IconLabelItem>()
                for (i in appList.indices) {
                    val app = appList[i]
                    items.add(IconLabelItem(app.icon, app.label)
                        .withIconSize(50)
                        .withTextColor(Color.WHITE)
                        .withIsAppLauncher(true)
                        .withIconPadding(8)
                        .withOnClickAnimate(false)
                        .withTextGravity(if (Setup.appSettings().getSearchUseGrid()) Gravity.CENTER else Gravity.CENTER_VERTICAL)
                        .withIconGravity(if (Setup.appSettings().getSearchUseGrid()) Gravity.TOP else Gravity.START)
                        .withOnClickListener { v ->
                            Tool.startApp(v.context, app, null)
                        }
                        .withOnLongClickListener(DragHandler.getLongClick(Item.newAppItem(app), DragAction.Action.SEARCH, null)))
                }
                _itemAdapter.set(items)

                return false
            }
        })

        _itemAdapter.itemFilter.filterPredicate = { item: IconLabelItem, constraint: CharSequence? ->
            if (constraint.isNullOrEmpty()) { true } else {

            var s = constraint.toString().lowercase()
            s = Normalizer.normalize(s, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            var itemLabel = item._label?.lowercase() ?: ""
            itemLabel = Normalizer.normalize(itemLabel, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

            if (Setup.appSettings().getSearchBarStartsWith()) {
                itemLabel!!.startsWith(s)
            } else {
                itemLabel!!.contains(s)
            }
        }
        }

        val recyclerParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        addView(_searchClock, clockParams)
        addView(_searchRecycler, recyclerParams)
        addView(_searchCardContainer, inputCardParams)
        addView(_searchButton, buttonParams)

        _searchInput.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                _searchInput.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val marginTop = Tool.dp2px(60)
                recyclerParams.setMargins(0, marginTop, 0, 0)
                _searchRecycler.layoutParams = recyclerParams
                _searchRecycler.setPadding(0, 0, 0, (bottomInset * 1.5).toInt())
            }
        })
    }

    private fun collapseInternal() {
        _callback?.onCollapse()

        _icon.setIcon(resources.getDrawable(R.drawable.ic_search))

        Tool.visibleViews(ANIM_TIME, _searchClock)
        Tool.goneViews(ANIM_TIME, _searchCardContainer, _searchRecycler, _switchButton)

        _searchInput.text?.clear()
    }

    private fun expandInternal() {
        _callback?.onExpand()

        _icon.setIcon(resources.getDrawable(R.drawable.ic_clear))

        Tool.visibleViews(ANIM_TIME, _searchCardContainer, _searchRecycler, _switchButton)
        Tool.goneViews(ANIM_TIME, _searchClock)
    }

    private fun updateSwitchIcon() {
        _switchButton.setImageResource(
            if (Setup.appSettings().getSearchUseGrid()) R.drawable.ic_view_grid_white
            else R.drawable.ic_view_list_white
        )
    }

    private fun updateRecyclerViewLayoutManager() {
        val gridSize = if (Setup.appSettings().getSearchUseGrid()) Setup.appSettings().getDrawerColumnCount() else 1
        if (gridSize == 1) {
            _searchRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            updateList(Gravity.START, Gravity.CENTER_VERTICAL)
        } else {
            _searchRecycler.layoutManager = GridLayoutManager(context, gridSize, GridLayoutManager.VERTICAL, false)
            updateList(Gravity.TOP, Gravity.CENTER)
        }
        _searchRecycler.layoutManager?.isAutoMeasureEnabled = false
    }

    private fun updateList(iconGravity: Int, textGravity: Int) {
        val apps = _itemAdapter.adapterItems
        for (app in apps) {
            app.setIconGravity(iconGravity)
            app.setTextGravity(textGravity)
        }
    }

    protected fun initRecyclerView() {
        _searchRecycler = RecyclerView(context)
        _searchRecycler.itemAnimator = null
        _searchRecycler.visibility = View.GONE
        _searchRecycler.adapter = _adapter
        _searchRecycler.clipToPadding = false
        _searchRecycler.setHasFixedSize(true)
        updateRecyclerViewLayoutManager()
    }

    val searchButton: AppCompatImageView
        get() = _searchButton

    fun updateClock() {
        val appSettings = AppSettings.get()
        _searchClock.setTextColor(appSettings.getDesktopDateTextColor())

        val now = ZonedDateTime.now()
        _clockFormatter = getSearchBarClockFormat(Setup.appSettings().getDesktopDateMode())

        val text = now.format(_clockFormatter)
        val lines = text.split("\n")
        if (lines.size < 2) {
            _searchClock.text = lines[0]
        } else {
            val span = SpannableString(text)
            span.setSpan(
                RelativeSizeSpan(_searchClockSubTextFactor),
                lines[0].length + 1,
                lines[0].length + 1 + lines[1].length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            _searchClock.text = span
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            bottomInset = insets.systemWindowInsetBottom
            setPadding(0, insets.systemWindowInsetTop, 0, 0)
            return insets
        }
        return insets
    }

    fun getSearchBarClockFormat(id: Int?): DateTimeFormatter? {
        if (_clockFormatterIndex != id && id != null && id > 0) {
            if (_clockModes.containsKey(id)) {
                return _clockModes[id]
            }
        }

        return Setup.appSettings().getUserDateFormat()
    }

    interface CallBack {
        fun onInternetSearch(string: String)
        fun onExpand()
        fun onCollapse()
    }
}
