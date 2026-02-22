package com.benny.openlauncher.interfaces

interface DialogListener {

    interface OnActionDialogListener {
        fun onAdd(type: Int)
    }

    interface OnEditDialogListener {
        fun onRename(name: String)
    }
}
