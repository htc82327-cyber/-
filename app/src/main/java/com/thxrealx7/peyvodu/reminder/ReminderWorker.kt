package com.thxrealx7.peyvodu.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.thxrealx7.peyvodu.data.peyVoduDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalTime

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.peyVoduDataStore.data.first()
        val enabled = prefs[booleanPreferencesKey("reminders")] ?: true
        if (!enabled) return Result.success()

        val start = prefs[intPreferencesKey("start_hour")] ?: 8
        val end = prefs[intPreferencesKey("end_hour")] ?: 22
        val hour = LocalTime.now().hour
        if (hour !in start until end) return Result.success()

        NotificationHelper.show(applicationContext)

        val voiceEnabled = prefs[booleanPreferencesKey("voice_reminders")] ?: true
        if (voiceEnabled) {
            val voice = prefs[stringPreferencesKey("voice")] ?: "calm"
            runCatching {
                VoicePlayer.play(applicationContext, VoicePlayer.reminderResource(), voice)
            }
        }
        return Result.success()
    }
}
