package com.thxrealx7.peyvodu

import android.app.Application
import com.thxrealx7.peyvodu.reminder.NotificationHelper

class PeyVoduApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
