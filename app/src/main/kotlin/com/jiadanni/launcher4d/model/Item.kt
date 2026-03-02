package com.jiadanni.launcher4d.model

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import com.jiadanni.launcher4d.util.Definitions
import com.jiadanni.launcher4d.util.Definitions.ItemPosition
import com.jiadanni.launcher4d.util.Tool
import kotlin.random.Random

class Item {
    // All items need these values
    var icon: Drawable? = null
    var label: String = ""
    var type: Type? = null
    var id: Int = Random.nextInt()
    var location: ItemPosition? = null
    var x: Int = 0
    var y: Int = 0

    // Intent for shortcuts and apps
    var intent: Intent? = null

    // List of shortcutInfo for shortcuts
    var shortcutInfo: List<ShortcutInfo>? = null

    // List of items for groups
    var items: MutableList<Item>? = null

    // Int value for launcher action
    var actionValue: Int = 0

    // Widget specific values
    var widgetValue: Int = 0
    var spanX: Int = 1
    var spanY: Int = 1

    enum class Type {
        APP,
        SHORTCUT,
        GROUP,
        ACTION,
        WIDGET
    }

    fun reset() {
        id = Random.nextInt()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Item) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

    // Backward compatibility with Java-style naming
    @Deprecated("Use icon property instead", ReplaceWith("icon"))
    var _icon: Drawable?
        get() = icon
        set(value) { icon = value }

    @Deprecated("Use label property instead", ReplaceWith("label"))
    var _label: String
        get() = label
        set(value) { label = value }

    @Deprecated("Use type property instead", ReplaceWith("type"))
    var _type: Type?
        get() = type
        set(value) { type = value }

    @Deprecated("Use id property instead", ReplaceWith("id"))
    var _id: Int
        get() = id
        set(value) { id = value }

    @Deprecated("Use location property instead", ReplaceWith("location"))
    var _location: ItemPosition?
        get() = location
        set(value) { location = value }

    @Deprecated("Use x property instead", ReplaceWith("x"))
    var _x: Int
        get() = x
        set(value) { x = value }

    @Deprecated("Use y property instead", ReplaceWith("y"))
    var _y: Int
        get() = y
        set(value) { y = value }

    @Deprecated("Use intent property instead", ReplaceWith("intent"))
    var _intent: Intent?
        get() = intent
        set(value) { intent = value }

    @Deprecated("Use shortcutInfo property instead", ReplaceWith("shortcutInfo"))
    var _shortcutInfo: List<ShortcutInfo>?
        get() = shortcutInfo
        set(value) { shortcutInfo = value }

    @Deprecated("Use items property instead", ReplaceWith("items"))
    var _items: MutableList<Item>?
        get() = items
        set(value) { items = value }

    @Deprecated("Use actionValue property instead", ReplaceWith("actionValue"))
    var _actionValue: Int
        get() = actionValue
        set(value) { actionValue = value }

    @Deprecated("Use widgetValue property instead", ReplaceWith("widgetValue"))
    var _widgetValue: Int
        get() = widgetValue
        set(value) { widgetValue = value }

    @Deprecated("Use spanX property instead", ReplaceWith("spanX"))
    var _spanX: Int
        get() = spanX
        set(value) { spanX = value }

    @Deprecated("Use spanY property instead", ReplaceWith("spanY"))
    var _spanY: Int
        get() = spanY
        set(value) { spanY = value }

    // Java-style getters/setters for backward compatibility
    fun getId(): Int = id
    fun setId(id: Int) { this.id = id }

    fun getIntent(): Intent? = intent
    fun setIntent(intent: Intent?) { this.intent = intent }

    fun getLabel(): String = label
    fun setLabel(label: String) { this.label = label }

    fun getType(): Type? = type
    fun setType(type: Type?) { this.type = type }

    fun getShortcutInfo(): List<ShortcutInfo>? = shortcutInfo
    fun setShortcutInfo(shortcutInfo: List<ShortcutInfo>?) { this.shortcutInfo = shortcutInfo }

    fun getGroupItems(): MutableList<Item>? = items
    fun getItems(): MutableList<Item>? = items
    fun setItems(items: MutableList<Item>?) { this.items = items }

    fun getX(): Int = x
    fun setX(x: Int) { this.x = x }

    fun getY(): Int = y
    fun setY(y: Int) { this.y = y }

    fun getSpanX(): Int = spanX
    fun setSpanX(spanX: Int) { this.spanX = spanX }

    fun getSpanY(): Int = spanY
    fun setSpanY(spanY: Int) { this.spanY = spanY }

    fun getIcon(): Drawable? = icon
    fun setIcon(icon: Drawable?) { this.icon = icon }

    fun getActionValue(): Int = actionValue
    fun setActionValue(actionValue: Int) { this.actionValue = actionValue }

    fun getWidgetValue(): Int = widgetValue
    fun setWidgetValue(widgetValue: Int) { this.widgetValue = widgetValue }

    companion object {
        @JvmStatic
        fun newAppItem(app: App): Item {
            return Item().apply {
                type = Type.APP
                label = app.label
                icon = app.icon
                intent = Tool.getIntentFromApp(app)
                shortcutInfo = app.shortcutInfo
            }
        }

        @JvmStatic
        fun newShortcutItem(intent: Intent, icon: Drawable?, name: String): Item {
            return Item().apply {
                type = Type.SHORTCUT
                label = name
                this.icon = icon
                spanX = 1
                spanY = 1
                this.intent = intent
            }
        }

        @JvmStatic
        fun newGroupItem(): Item {
            return Item().apply {
                type = Type.GROUP
                label = ""
                spanX = 1
                spanY = 1
                items = ArrayList()
            }
        }

        @JvmStatic
        fun newActionItem(action: Int): Item {
            return Item().apply {
                type = Type.ACTION
                spanX = 1
                spanY = 1
                actionValue = action
            }
        }

        @JvmStatic
        fun newWidgetItem(componentName: ComponentName, widgetValue: Int): Item {
            return Item().apply {
                type = Type.WIDGET
                label = "${componentName.packageName}${Definitions.DELIMITER}${componentName.className}"
                this.widgetValue = widgetValue
                spanX = 1
                spanY = 1
            }
        }
    }
}
