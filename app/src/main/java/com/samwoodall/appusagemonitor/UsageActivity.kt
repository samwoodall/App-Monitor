package com.samwoodall.appusagemonitor

import android.app.usage.UsageStatsManager
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.app.AppOpsManager
import android.content.Intent
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.view.View
import kotlinx.android.synthetic.main.activity_usage.*

class UsageActivity : AppCompatActivity(), UsageView {

    var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)

        recyclerView = usage_recycler_view
        recyclerView?.setHasFixedSize(true)
        recyclerView?.layoutManager = LinearLayoutManager(this)

        set_permission_button.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

    }

    fun checkForUsageAccessSettingsPermission() : Boolean {
        val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), this.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun determineWhetherToGetUsageOrGetPermission() {
        if (checkForUsageAccessSettingsPermission()) {
            getUsageStats()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.dialog_ask_for_permission)
                    .setPositiveButton(R.string.ok, { dialog, id ->
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    })
                    .setNegativeButton(R.string.cancel, { dialog, id ->

                    }).setOnDismissListener({
                        set_permission_text.visibility = View.VISIBLE
                        set_permission_button.visibility = View.VISIBLE
            }).show()
        }
    }

    override fun onResume() {
        super.onResume()
        recyclerView?.visibility = View.GONE
        set_permission_button.visibility = View.GONE
        set_permission_text.visibility = View.GONE
        determineWhetherToGetUsageOrGetPermission()
    }

    fun getUsageStats () {
        recyclerView?.visibility = View.VISIBLE
        val usageStatsManager = this.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryAndAggregateUsageStats(0, System.currentTimeMillis() )
        val listStats = stats.toList()

        val appNames = listStats.map { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.first, 0))}
        val appIcon = listStats.map { packageManager.getApplicationIcon(packageManager.getApplicationInfo(it.first, 0))}
        val totalForegroundTimes = listStats.map { TimeUnit.MILLISECONDS.toMinutes(it.second.totalTimeInForeground) }

        val simpleDateFormat = SimpleDateFormat("dd MMM h:mma", Locale.getDefault())

        val lastUsedTimes = listStats.map { if (it.second.lastTimeUsed.toInt() != 0)
            getString(R.string.last_used, simpleDateFormat.format(it.second.lastTimeUsed)) else getString(R.string.never_used) }

        val usefulInfo = mutableListOf<UsageInfo>()

        for (i in appNames.indices) {
            usefulInfo += UsageInfo(appNames[i], appIcon[i], totalForegroundTimes[i], lastUsedTimes[i])
        }

        usefulInfo.sortByDescending { it.totalForegroundTime }

        recyclerView?.adapter = UsageAdapter(usefulInfo.toList())
    }
}
