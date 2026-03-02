package com.jiadanni.launcher4d.model

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle

data class App(
    var icon: Drawable,
    val label: String,
    val packageName: String,
    val className: String,
    val userHandle: UserHandle? = null,
    val shortcutInfo: List<ShortcutInfo>? = null
) {
    constructor(pm: PackageManager, info: ResolveInfo, shortcutInfo: List<ShortcutInfo>?) : this(
        icon = info.loadIcon(pm),
        label = info.loadLabel(pm).toString(),
        packageName = info.activityInfo.packageName,
        className = info.activityInfo.name,
        shortcutInfo = shortcutInfo
    )

    @SuppressLint("NewApi")
    constructor(pm: PackageManager, info: LauncherActivityInfo, shortcutInfo: List<ShortcutInfo>?) : this(
        icon = info.getIcon(0),
        label = info.label.toString(),
        packageName = info.componentName.packageName,
        className = info.name,
        shortcutInfo = shortcutInfo
    )

    val componentName: String
        get() = ComponentName(packageName, className).toString()

    // Override equals to match Java implementation (compare by packageName only)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is App) return false
        return packageName == other.packageName
    }

    // Override hashCode to be consistent with equals
    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    // Preserve Java-style naming for backward compatibility
    @Deprecated("Use icon property instead", ReplaceWith("icon"))
    var _icon: Drawable
        get() = icon
        set(value) { icon = value }

    @Deprecated("Use label property instead", ReplaceWith("label"))
    val _label: String get() = label

    @Deprecated("Use packageName property instead", ReplaceWith("packageName"))
    val _packageName: String get() = packageName

    @Deprecated("Use className property instead", ReplaceWith("className"))
    val _className: String get() = className

    @Deprecated("Use userHandle property instead", ReplaceWith("userHandle"))
    val _userHandle: UserHandle? get() = userHandle

    @Deprecated("Use shortcutInfo property instead", ReplaceWith("shortcutInfo"))
    val _shortcutInfo: List<ShortcutInfo>? get() = shortcutInfo

    // Keep Java-style getters for backward compatibility
    fun getIcon(): Drawable = icon
    fun setIcon(icon: Drawable) { this.icon = icon }
    fun getLabel(): String = label
    fun getPackageName(): String = packageName
    fun getClassName(): String = className
    fun getComponentName(): String = componentName
    fun getShortcutInfo(): List<ShortcutInfo>? = shortcutInfo
}
