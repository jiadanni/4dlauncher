package com.jiadanni.launcher4d.interfaces

interface DialogListener {

    interface OnActionDialogListener {
        fun onAdd(type: Int)
    }

    interface OnEditDialogListener {
        fun onRename(name: String)
    }
}
