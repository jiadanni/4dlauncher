package com.benny.openlauncher.activity.homeparts

import android.content.Context
import com.benny.openlauncher.interfaces.DialogListener
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.util.LauncherAction
import com.benny.openlauncher.viewutil.DialogHelper

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
