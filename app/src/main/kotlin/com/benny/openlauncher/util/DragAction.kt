package com.benny.openlauncher.util

data class DragAction(var action: Action) {
    enum class Action {
        DESKTOP, DRAWER, SEARCH
    }
}
