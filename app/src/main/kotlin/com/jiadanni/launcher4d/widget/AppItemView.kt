package com.jiadanni.launcher4d.widget

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.notifications.NotificationListener
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.viewutil.GroupDrawable

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), NotificationListener.NotificationCallback {

    var icon: Drawable? = null
        set(value) {
            field = value
            updateCompoundDrawables()
        }

    var label: String? = null
        set(value) {
            field = value
            text = if (showLabel) value else ""
        }

    private val notifyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val notifyPaint = Paint()

    var iconSize: Float = 0f
        set(value) {
            field = value
            updateCompoundDrawables()
        }

    var showLabel: Boolean = true
        set(value) {
            field = value
            text = if (value) label else ""
        }

    private var notificationCount = 0

    init {
        gravity = Gravity.CENTER
        notifyTextPaint.color = Color.WHITE
        notifyPaint.color = Color.RED
    }

    override fun notificationCallback(count: Int) {
        notificationCount = count
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (notificationCount > 0 && icon != null) {
            val count = if (notificationCount > 99) "++" else notificationCount.toString()
            val radius = iconSize * 0.15f

            val iconLeft = (width - iconSize) / 2f
            val iconTop = paddingTop.toFloat()

            canvas.save()
            canvas.translate(iconLeft, iconTop)

            canvas.drawCircle(iconSize - radius, radius, radius, notifyPaint)

            notifyTextPaint.textSize = radius * 1.5f

            canvas.drawText(
                count,
                iconSize - radius - (notifyTextPaint.measureText(count) / 2),
                radius - ((notifyTextPaint.descent() + notifyTextPaint.ascent()) / 2),
                notifyTextPaint
            )
            canvas.restore()
        }
    }

    private fun updateCompoundDrawables() {
        val size = iconSize.toInt()
        if (size > 0 && icon != null) {
            icon?.setBounds(0, 0, size, size)
            setCompoundDrawables(null, icon, null, null)
        } else {
            setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)
        }
    }

    class Builder {
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
                        AppManager.getInstance(view.context).findApp(item._intent)!!,
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

                    if (id.isNullOrEmpty()) {
                        intent?.let { view.context.startActivity(it) }
                    } else {
                        val launcherApps = view.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                        val packageName = intent.getPackage()!!
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
            view.setTextColor(color)
            return this
        }

        fun setIconSize(iconSize: Int): Builder {
            view.iconSize = Tool.dp2px(iconSize).toFloat()
            return this
        }

        fun setLabelVisibility(visible: Boolean): Builder {
            view.showLabel = visible
            return this
        }

        fun vibrateWhenLongPress(vibrate: Boolean): Builder {
            return this
        }
    }
}
