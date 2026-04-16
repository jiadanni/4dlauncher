package com.jiadanni.launcher4d.viewutil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.widget.AppDrawerGrid
import com.jiadanni.launcher4d.widget.AppItemView
import com.mikepenz.fastadapter.items.AbstractItem

class IconLabelItem : AbstractItem<IconLabelItem.ViewHolder> {
    private var width = Int.MAX_VALUE
    private var height = Int.MAX_VALUE

    var icon: Drawable? = null
    private var iconSize = Int.MAX_VALUE
    private var iconGravity = 0
    private var iconPadding = 0
    private var iconColor = 0

    var label: String? = null
    private var textGravity = Gravity.CENTER_VERTICAL
    private var textColor = Int.MAX_VALUE
    private var textVisibility = true
    private var isAppLauncher = false

    private var onClickAnimate = true
    private var onClickListener: View.OnClickListener? = null
    private var onLongClickListener: View.OnLongClickListener? = null

    constructor(context: Context, icon: Int, label: Int) {
        this.label = context.getString(label)
        this.icon = context.resources.getDrawable(icon, context.theme)
    }

    constructor(icon: Drawable?, label: String?) {
        this.label = label
        this.icon = icon
    }

    constructor(app: App) {
        this.label = app.label
        this.icon = app.icon
        withWidth(AppDrawerGrid._itemWidth)
        withIconSize(Setup.appSettings().iconSize)
        withTextVisibility(Setup.appSettings().drawerShowLabel)
        withTextColor(Setup.appSettings().drawerLabelColor)
        withIconPadding(8)
        withTextGravity(Gravity.CENTER)
        withIconGravity(Gravity.TOP)
        withOnClickAnimate(false)
        withIsAppLauncher(true)
        withOnClickListener { v ->
            Tool.startApp(v.context, app, null)
        }
        withOnLongClickListener(DragHandler.getLongClick(Item.newAppItem(app), DragAction.Action.DRAWER, null))
    }

    fun withWidth(width: Int): IconLabelItem {
        this.width = width
        return this
    }

    fun withIconSize(iconSize: Int): IconLabelItem {
        this.iconSize = Tool.dp2px(iconSize.toFloat())
        return this
    }

    fun withIconColor(iconColor: Int): IconLabelItem {
        this.iconColor = iconColor
        return this
    }

    fun withIconGravity(iconGravity: Int): IconLabelItem {
        this.iconGravity = iconGravity
        return this
    }

    fun withIconPadding(iconPadding: Int): IconLabelItem {
        this.iconPadding = Tool.dp2px(iconPadding.toFloat())
        return this
    }

    fun withTextGravity(textGravity: Int): IconLabelItem {
        this.textGravity = textGravity
        return this
    }

    fun withTextColor(textColor: Int): IconLabelItem {
        this.textColor = textColor
        return this
    }

    fun withTextVisibility(visibility: Boolean): IconLabelItem {
        this.textVisibility = visibility
        return this
    }

    fun withOnClickAnimate(background: Boolean): IconLabelItem {
        this.onClickAnimate = background
        return this
    }

    fun withOnClickListener(listener: View.OnClickListener?): IconLabelItem {
        this.onClickListener = listener
        return this
    }

    fun withOnLongClickListener(onLongClickListener: View.OnLongClickListener?): IconLabelItem {
        this.onLongClickListener = onLongClickListener
        return this
    }

    fun withIsAppLauncher(isAppLauncher: Boolean): IconLabelItem {
        this.isAppLauncher = isAppLauncher
        return this
    }

    // Only used for search bar
    fun setIconGravity(iconGravity: Int) {
        this.iconGravity = iconGravity
    }

    fun setTextGravity(textGravity: Int) {
        this.textGravity = textGravity
    }

    override fun getViewHolder(view: View): ViewHolder {
        return ViewHolder(view, this)
    }

    override val layoutRes: Int get() {
        return R.layout.item_icon_label
    }

    override val type: Int get() {
        return R.id.id_adapter_icon_label_item
    }

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        // Set width and height
        holder.itemView.layoutParams.width = if (width == Int.MAX_VALUE) {
            RecyclerView.LayoutParams.MATCH_PARENT
        } else {
            width
        }

        holder.itemView.layoutParams.height = if (height == Int.MAX_VALUE) {
            RecyclerView.LayoutParams.WRAP_CONTENT
        } else {
            height
        }

        if (holder.itemView is AppItemView) {
            val appItemView = holder.itemView as AppItemView
            appItemView.iconSize = iconSize.toFloat()
            appItemView.showLabel = textVisibility
            appItemView.label = label
            appItemView.icon = icon
            appItemView.gravity = textGravity
            appItemView.setTextColor(if (textColor != Int.MAX_VALUE) textColor else appItemView.textColors.defaultColor)
        } else {
            val textView = holder.itemView as TextView
            // Only run all this code if a label should be shown
            if (label != null && textVisibility) {
                textView.text = label
                textView.gravity = textGravity
                textView.maxLines = 1
                textView.ellipsize = TextUtils.TruncateAt.END
                // No default text color since it will be set by the theme
                if (textColor != Int.MAX_VALUE) {
                    textView.setTextColor(textColor)
                }
            } else {
                textView.text = ""
            }

            // Icon specific padding
            textView.compoundDrawablePadding = iconPadding
            var finalIcon = icon
            if (iconSize != Int.MAX_VALUE && finalIcon != null) {
                val bitmap = Tool.drawableToBitmap(finalIcon)
                bitmap?.let {
                    finalIcon = BitmapDrawable(
                        Setup.appContext().resources,
                        Bitmap.createScaledBitmap(it, iconSize, iconSize, true)
                    )
                    finalIcon?.setColorFilter(iconColor, PorterDuff.Mode.SRC_ATOP)
                    if (isAppLauncher) {
                        finalIcon?.setBounds(0, 0, iconSize, iconSize)
                    }
                }
            }

            // Set compound drawables based on gravity
            when (iconGravity) {
                Gravity.START -> {
                    if (isAppLauncher) {
                        textView.setCompoundDrawables(finalIcon, null, null, null)
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(finalIcon, null, null, null)
                    }
                }
                Gravity.END -> {
                    if (isAppLauncher) {
                        textView.setCompoundDrawables(null, null, finalIcon, null)
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, finalIcon, null)
                    }
                }
                Gravity.TOP -> {
                    if (isAppLauncher) {
                        textView.setCompoundDrawables(null, finalIcon, null, null)
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(null, finalIcon, null, null)
                    }
                }
                Gravity.BOTTOM -> {
                    if (isAppLauncher) {
                        textView.setCompoundDrawables(null, null, null, finalIcon)
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, finalIcon)
                    }
                }
            }
        }

        // Most items will not use a long click
        if (!onClickAnimate) {
            holder.itemView.setBackgroundResource(0)
        }
        onClickListener?.let { holder.itemView.setOnClickListener(it) }
        onLongClickListener?.let { holder.itemView.setOnLongClickListener(it) }

        super.bindView(holder, payloads)
    }

    // Backward compatibility with Java naming
    @Deprecated("Use icon property", ReplaceWith("icon"))
    var _icon: Drawable?
        get() = icon
        set(value) { icon = value }

    @Deprecated("Use label property", ReplaceWith("label"))
    var _label: String?
        get() = label
        set(value) { label = value }

    class ViewHolder(itemView: View, item: IconLabelItem) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.tag = item
        }
    }
}
