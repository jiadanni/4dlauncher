package com.jiadanni.launcher4d.fragment

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.ViewSwitcher
import androidx.fragment.app.Fragment
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.AppSettings
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HideAppsFragment : Fragment() {

    private val listActivitiesHidden = ArrayList<String>()
    private val listActivitiesAll = ArrayList<App>()
    private var taskList = AsyncWorkerList()
    private var appInfoAdapter: HideAppsAdapter? = null
    private var switcherLoad: ViewSwitcher? = null
    private var grid: ListView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.view_hide_apps, container, false)
        switcherLoad = rootView.findViewById(R.id.viewSwitcherLoadingMain)

        val fab = rootView.findViewById<FloatingActionButton>(R.id.fab_rq)
        fab.setOnClickListener {
            confirmSelection()
        }

        when (taskList.status) {
            AsyncTask.Status.PENDING -> {
                // task has not started yet
                taskList.execute()
            }
            AsyncTask.Status.FINISHED -> {
                // task is done and onPostExecute has been called
                AsyncWorkerList().execute()
            }
            else -> {
                // Running, do nothing
            }
        }

        return rootView
    }

    @Suppress("DEPRECATION")
    inner class AsyncWorkerList : AsyncTask<String, Int, String?>() {

        override fun onPreExecute() {
            val hiddenList = AppSettings.get().hiddenAppsList ?: emptyList()
            listActivitiesHidden.addAll(hiddenList)
            super.onPreExecute()
        }

        override fun doInBackground(vararg params: String?): String? {
            return try {
                // compare to installed apps
                prepareData()
                null
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
        }

        override fun onPostExecute(result: String?) {
            populateView()
            // switch from loading screen to the main view
            switcherLoad?.showNext()
            super.onPostExecute(result)
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun confirmSelection() {
        val actionSendThread = Thread {
            // update hidden apps
            AppSettings.get().hiddenAppsList = listActivitiesHidden
            activity?.finish()
        }

        if (!actionSendThread.isAlive) {
            // prevents thread from being executed more than once
            actionSendThread.start()
        }
    }

    private fun prepareData() {
        val apps = AppManager.getInstance(requireContext()).nonFilteredApps
        listActivitiesAll.addAll(apps)
    }

    private fun populateView() {
        grid = activity?.findViewById(R.id.app_grid)

        grid?.apply {
            isFastScrollEnabled = true
            isFastScrollAlwaysVisible = false
        }

        appInfoAdapter = HideAppsAdapter(requireActivity(), listActivitiesAll)

        grid?.adapter = appInfoAdapter
        grid?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, position, _ ->
            val appInfo = adapterView.getItemAtPosition(position) as App
            val checker = view.findViewById<CheckBox>(R.id.checkbox)
            val icon = view.findViewById<ViewSwitcher>(R.id.viewSwitcherChecked)

            checker.toggle()
            if (checker.isChecked) {
                listActivitiesHidden.add(appInfo.componentName)
                if (DEBUG) Log.v(TAG, "Selected App: ${appInfo.label}")
                if (icon.displayedChild == 0) {
                    icon.showNext()
                }
            } else {
                listActivitiesHidden.remove(appInfo.componentName)
                if (DEBUG) Log.v(TAG, "Deselected App: ${appInfo.label}")
                if (icon.displayedChild == 1) {
                    icon.showPrevious()
                }
            }
        }
    }

    private inner class HideAppsAdapter(
        context: Context,
        adapterArrayList: ArrayList<App>
    ) : ArrayAdapter<App>(context, R.layout.item_hide_apps, adapterArrayList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = (requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                    .inflate(R.layout.item_hide_apps, parent, false)
                holder = ViewHolder(view)
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val appInfo = getItem(position)!!

            holder.apkPackage.text = appInfo.className
            holder.apkName.text = appInfo.label
            holder.apkIcon.setImageDrawable(appInfo.icon)

            holder.switcherChecked.inAnimation = null
            holder.switcherChecked.outAnimation = null
            holder.checker.isChecked = listActivitiesHidden.contains(appInfo.componentName)

            if (listActivitiesHidden.contains(appInfo.componentName)) {
                if (holder.switcherChecked.displayedChild == 0) {
                    holder.switcherChecked.showNext()
                }
            } else {
                if (holder.switcherChecked.displayedChild == 1) {
                    holder.switcherChecked.showPrevious()
                }
            }

            return view
        }
    }

    private class ViewHolder(view: View) {
        val apkName: TextView = view.findViewById(R.id.appName)
        val apkPackage: TextView = view.findViewById(R.id.appPackage)
        val apkIcon: ImageView = view.findViewById(R.id.appIcon)
        val checker: CheckBox = view.findViewById(R.id.checkbox)
        val switcherChecked: ViewSwitcher = view.findViewById(R.id.viewSwitcherChecked)
    }

    companion object {
        private const val TAG = "RequestActivity"
        private const val DEBUG = true
    }
}
