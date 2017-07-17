package com.samwoodall.appusagemonitor

import android.graphics.drawable.Drawable

data class UsageInfo(val appName: CharSequence,
                     val appIcon: Drawable,
                     val totalForegroundTime: Long,
                     val lastUsedTime: String)