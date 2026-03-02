package com.jiadanni.launcher4d.util

object Definitions {
    const val BUFFER_SIZE = 2048
    const val INTENT_BACKUP = 5
    const val INTENT_RESTORE = 3
    const val ACTION_LAUNCHER = 8

    // separates a list of integers
    const val DELIMITER = "#"

    // DO NOT REARRANGE
    // enum ordinal used for db
    enum class ItemPosition {
        Dock,
        Desktop,
        Group
    }

    enum class ItemState {
        Hidden,
        Visible
    }

    enum class WallpaperScroll {
        Normal,
        Inverse,
        Off
    }
}
