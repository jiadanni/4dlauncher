package com.jiadanni.launcher4d.widget

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.notifications.NotificationListener
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.viewutil.GroupDrawable
import kotlin.math.ceil

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Drawable.Callback, NotificationListener.NotificationCallback {

    var icon: Drawable? = null
    var label: String? = null
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val notifyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val notifyPaint = Paint()
    private val textContainer = Rect()
    private val testTextContainer = Rect()
    var iconSize: Float = 0f
    private var showLabel = true
    private var vibrateWhenLongPress = false
    private val labelHeight: Float
    private var targetedWidth = 0
    private var targetedHeightPadding = 0
    private var heightPadding: Float = 0f

    private var notificationCount = 0

    init {
        labelHeight = Tool.dp2px(14)
        textPaint.textSize = Tool.sp2px(12)
        textPaint.color = Color.WHITE
        notifyTextPaint.color = Color.WHITE
        notifyPaint.color = Color.RED
    }

    override fun notificationCallback(count: Int) {
        notificationCount = count
        invalidate()
    }

    fun setTargetedWidth(width: Int) {
        targetedWidth = width
    }

    fun setTargetedHeightPadding(padding: Int) {
        targetedHeightPadding = padding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mWidth = iconSize
        val mHeight = iconSize + if (showLabel) labelHeight else 0f
        if (targetedWidth != 0) {
            mWidth = targetedWidth.toFloat()
        }
        setMeasuredDimension(
            ceil(mWidth).toInt(),
            ceil(mHeight.toInt().toDouble()).toInt() + Tool.dp2px(2) + targetedHeightPadding * 2
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        heightPadding = (height - iconSize - if (showLabel) labelHeight else 0f) / 2f

        if (label != null && showLabel) {
            val currentLabel = label!!
            textPaint.getTextBounds(currentLabel, 0, currentLabel.length, textContainer)
            val maxTextWidth = width - MIN_ICON_TEXT_MARGIN * 2

            // use ellipsis if the label is too long
            if (textContainer.width() > maxTextWidth) {
                val testLabel = currentLabel + ELLIPSIS
                textPaint.getTextBounds(testLabel, 0, testLabel.length, testTextContainer)

                // Premeditate to be faster
                val characterSize = testTextContainer.width() / testLabel.length.toFloat()
                val charsToTruncate = ((testTextContainer.width() - maxTextWidth) / characterSize).toInt()

                canvas.drawText(
                    currentLabel.substring(0, currentLabel.length - charsToTruncate) + ELLIPSIS,
                    MIN_ICON_TEXT_MARGIN.toFloat(),
                    height - heightPadding,
                    textPaint
                )
            } else {
                canvas.drawText(
                    currentLabel,
                    (width - textContainer.width()) / 2f,
                    height - heightPadding,
                    textPaint
                )
            }
        }

        // center the icon
        icon?.let { drawable ->
            canvas.save()
            canvas.translate((width - iconSize) / 2, heightPadding)
            drawable.setBounds(0, 0, iconSize.toInt(), iconSize.toInt())
            drawable.draw(canvas)

            if (notificationCount > 0) {
                val count = if (notificationCount > 99) "++" else notificationCount.toString()
                val radius = iconSize * 0.15f
                canvas.drawCircle(iconSize - radius, radius, radius, notifyPaint)

                notifyTextPaint.textSize = (radius * 1.5f).toInt().toFloat()

                canvas.drawText(
                    count,
                    iconSize - radius - (notifyTextPaint.measureText(count) / 2),
                    radius - ((notifyTextPaint.descent() + notifyTextPaint.ascent()) / 2),
                    notifyTextPaint
                )
            }

            canvas.restore()
        }
    }

    val drawIconTop: Float
        get() = heightPadding

    val drawIconLeft: Float
        get() = (width - iconSize) / 2

    class Builder {
        // TODO accept any view and just add click and long click listeners
        // this class isn't necessary
        // remove in favor of using ItemViewFactory
        private val view: AppItemView

        constructor(context: Context) {
            view = AppItemView(context)
        }

        constructor(view: AppItemView) {
            this.view = view
        }

        fun getView(): AppItemView = view

        fun setAppItem(item: Item): Builder {
            view.label = item.label
            view.icon = item.icon
            view.setOnClickListener {
                Tool.createScaleInScaleOutAnim(view) {
                    Tool.startApp(
                        view.context,
                        AppManager.getInstance(view.context).findApp(item._intent),
                        view
                    )
                }
            }
            return this
        }

        fun setShortcutItem(item: Item): Builder {
            view.label = item.label
            view.icon = item.icon
            view.setOnClickListener {
                Tool.createScaleInScaleOutAnim(view) {
                    val intent = item.intent
                    val id = intent?.getStringExtra("shortcut_id")

                    /* old style shortcut */
                    if (id.isNullOrEmpty()) {
                        intent?.let { view.context.startActivity(it) }
                    }
                    /* new style shortcut */
                    else {
                        val launcherApps = view.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                        val packageName = intent.getPackage()
                        launcherApps.startShortcut(packageName, id, intent.sourceBounds, null, Process.myUserHandle())
                    }
                }
            }
            return this
        }

        fun setGroupItem(context: Context, callback: DesktopCallback, item: Item): Builder {
            view.label = item.label
            view.icon = GroupDrawable(context, item, Setup.appSettings().iconSize)
            view.setOnClickListener { v ->
                Tool.getLauncher(v.context)?.let { launcher ->
                    if (launcher.groupPopup.showPopup(item, v, callback)) {
                        ((v as AppItemView).icon as? GroupDrawable)?.popUp()
                    }
                }
            }
            return this
        }

        fun setActionItem(item: Item): Builder {
            view.label = item.label
            view.icon = ContextCompat.getDrawable(Setup.appContext(), R.drawable.item_drawer)
            view.setOnClickListener {
                Tool.createScaleInScaleOutAnim(view) {
                    Tool.getLauncher(view.context)?.openAppDrawer(view, 0, 0)
                }
            }
            return this
        }

        fun withOnLongClick(item: Item, action: DragAction.Action, desktopCallback: DesktopCallback?): Builder {
            view.setOnLongClickListener(DragHandler.getLongClick(item, action, desktopCallback))
            return this
        }

        fun setTextColor(color: Int): Builder {
            view.textPaint.color = color
            return this
        }

        fun setIconSize(iconSize: Int): Builder {
            view.iconSize = Tool.dp2px(iconSize)
            return this
        }

        fun setLabelVisibility(visible: Boolean): Builder {
            view.showLabel = visible
            return this
        }

        fun vibrateWhenLongPress(vibrate: Boolean): Builder {
            view.vibrateWhenLongPress = vibrate
            return this
        }
    }

    companion object {
        private const val MIN_ICON_TEXT_MARGIN = 8
        private const val ELLIPSIS = '…'
    }
}
