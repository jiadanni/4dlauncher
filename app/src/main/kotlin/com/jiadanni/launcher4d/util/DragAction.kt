package com.jiadanni.launcher4d.util

data class DragAction(var action: Action) {
    enum class Action {
        DESKTOP, DRAWER, SEARCH
    }
}
