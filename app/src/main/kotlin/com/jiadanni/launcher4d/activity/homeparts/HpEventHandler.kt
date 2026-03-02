package com.jiadanni.launcher4d.activity.homeparts

import android.content.Context
import com.jiadanni.launcher4d.interfaces.DialogListener
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.Definitions
import com.jiadanni.launcher4d.util.LauncherAction
import com.jiadanni.launcher4d.viewutil.DialogHelper

class HpEventHandler : Setup.EventHandler {
    override fun showLauncherSettings(context: Context) {
        LauncherAction.RunAction(LauncherAction.Action.LauncherSettings, context)
    }

    override fun showPickAction(context: Context, listener: DialogListener.OnActionDialogListener) {
        DialogHelper.selectDesktopActionDialog(context) { _, _, position, _ ->
            if (position == 0) {
                listener.onAdd(Definitions.ACTION_LAUNCHER)
            }
        }
    }

    override fun showEditDialog(context: Context, item: Item, listener: DialogListener.OnEditDialogListener) {
        DialogHelper.editItemDialog("Edit Item", item.label, context, object : DialogHelper.OnItemEditListener {
            override fun itemLabel(label: String) {
                listener.onRename(label)
            }
        })
    }

    override fun showDeletePackageDialog(context: Context, item: Item) {
        DialogHelper.deletePackageDialog(context, item)
    }
}
