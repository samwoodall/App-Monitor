package com.samwoodall.appusagemonitor

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.Adapter
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.usage_item.view.*

class UsageAdapter(val usageData: List<UsageInfo>) : Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return UsageViewHolder(parent.inflate(R.layout.usage_item))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as UsageViewHolder
        holder.bind(usageData[position])
    }
    override fun getItemCount() = usageData.size
}

class UsageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(usageInfo: UsageInfo) = with(itemView) {
        icon.setImageDrawable(usageInfo.appIcon)
        name.text = usageInfo.appName
        total_foreground_time.text = context.getString(R.string.total_foreground_time, usageInfo.totalForegroundTime.toString())
        last_time_used.text = usageInfo.lastUsedTime
    }
}


