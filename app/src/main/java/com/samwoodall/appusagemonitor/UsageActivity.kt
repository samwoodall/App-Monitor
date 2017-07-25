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
import android.content.DialogInterface
import android.content.Intent
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_usage.*

class UsageActivity : AppCompatActivity(), UsageView {

    var recyclerView: RecyclerView? = null
    var lastUsedTimeRange = UsageRange.ALL_TIME
    var lastUsedSort = UsageSort.MOST_USED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)

        recyclerView = usage_recycler_view
        recyclerView?.layoutManager = LinearLayoutManager(this)

        set_permission_button.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    fun checkForUsageAccessSettingsPermission() : Boolean {
        val appOps = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), this.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun setNeedPermission() {
        recyclerView?.visibility = View.GONE
        set_permission_button.visibility = View.GONE
        set_permission_text.visibility = View.GONE


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

    override fun onResume() {
        super.onResume()
        getUsageStats(lastUsedTimeRange, lastUsedSort)
    }

    fun getUsageStats (usageRange: UsageRange = UsageRange.ALL_TIME, usageSort: UsageSort = UsageSort.MOST_USED) {
        if (checkForUsageAccessSettingsPermission()) {
            recyclerView?.visibility = View.VISIBLE
            val usageStatsManager = this.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val stats = usageStatsManager.queryAndAggregateUsageStats(System.currentTimeMillis() - usageRange.timePeriod, System.currentTimeMillis() )
            val listStats = stats.toList()

            val appNames = listStats.map { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it.first, 0))}
            val appIcon = listStats.map { packageManager.getApplicationIcon(packageManager.getApplicationInfo(it.first, 0))}
            val totalForegroundTimes = listStats.map { TimeUnit.MILLISECONDS.toMinutes(it.second.totalTimeInForeground) }

            val simpleDateFormat = SimpleDateFormat("dd MMM h:mma", Locale.getDefault())

            val lastUsedTimes = listStats.map { if (it.second.lastTimeUsed.toInt() != 0)
                getString(R.string.last_used, simpleDateFormat.format(it.second.lastTimeUsed)) else getString(R.string.never_used) }

            val sortableLastUsedTimes = listStats.map { it.second.lastTimeUsed }
            val sortableLastUsedWithGivenMaxValue = sortableLastUsedTimes.map { if (it.toInt() == 0) sortableLastUsedTimes.max()?.plus(1) else it }

            val usefulInfo = mutableListOf<UsageInfo>()

//            for (i in appNames.indices) {
//                usefulInfo += UsageInfo(appNames[i], appIcon[i], totalForegroundTimes[i], lastUsedTimes[i], sortableLastUsedTimes[i])
//            }
            for (i in appNames.indices) {
                usefulInfo += UsageInfo(appNames[i], appIcon[i], totalForegroundTimes[i], lastUsedTimes[i])
            }

            when(usageSort) {
                UsageActivity.UsageSort.MOST_USED -> usefulInfo.sortByDescending { it.totalForegroundTime }
                UsageActivity.UsageSort.MOST_RECENT -> TODO()
            }


            recyclerView?.adapter = UsageAdapter(usefulInfo.toList())
        } else {
            setNeedPermission()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_why_permission -> {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.dialog_permission_explanation)
                        .setPositiveButton(R.string.ok, { dialog, id ->
                        }).show()
                return true
            }

            R.id.action_set_time_range -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.dialog_set_time_range).setSingleChoiceItems(
                        R.array.string_array_date_range, lastUsedTimeRange.ordinal, DialogInterface.OnClickListener { dialog, which ->
                    lastUsedTimeRange = UsageRange.values()[which]
                }).setNegativeButton(R.string.cancel, { dialog, id ->
                }).setPositiveButton(R.string.ok, { dialog, id ->
                    when(lastUsedTimeRange.ordinal) {
                        0 -> getUsageStats(UsageRange.DAY, lastUsedSort)
                        1 -> getUsageStats(UsageRange.WEEK, lastUsedSort)
                        2 -> getUsageStats(UsageRange.MONTH, lastUsedSort)
                        3 -> getUsageStats(UsageRange.YEAR, lastUsedSort)
                        4 -> getUsageStats(UsageRange.ALL_TIME, lastUsedSort)
                    }
                }).show()
                return true
            }

            R.id.action_sort -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.dialog_sort_by).setNegativeButton(R.string.cancel, { dialog, id ->
                }).setSingleChoiceItems(R.array.string_array_sort, lastUsedSort.ordinal, DialogInterface.OnClickListener { dialog, which ->
                    lastUsedSort = UsageSort.values()[which]
                }).setPositiveButton(R.string.ok, { dialog, id ->
                    when(lastUsedSort.ordinal) {
                        0 -> getUsageStats(lastUsedTimeRange, UsageSort.MOST_USED)
                        1 -> getUsageStats(lastUsedTimeRange, UsageSort.MOST_RECENT)
                    }
                }).show()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    enum class UsageRange(val timePeriod: Long) {
        DAY(DateUtils.DAY_IN_MILLIS),
        WEEK(DateUtils.WEEK_IN_MILLIS),
        MONTH(DateUtils.WEEK_IN_MILLIS * 4),
        YEAR(DateUtils.YEAR_IN_MILLIS),
        ALL_TIME(System.currentTimeMillis())
    }

    enum class UsageSort {
        MOST_USED, MOST_RECENT
    }
}
