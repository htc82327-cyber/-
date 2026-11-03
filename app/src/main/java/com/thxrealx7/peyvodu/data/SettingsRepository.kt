package com.thxrealx7.peyvodu.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.peyVoduDataStore by preferencesDataStore(name = "pey_vodu_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboarding")
        val name = stringPreferencesKey("name")
        val age = intPreferencesKey("age")
        val gender = stringPreferencesKey("gender")
        val height = intPreferencesKey("height")
        val weight = intPreferencesKey("weight")
        val mode = stringPreferencesKey("mode")
        val glass = intPreferencesKey("glass")
        val goal = intPreferencesKey("goal")
        val water = intPreferencesKey("water")
        val waterDate = stringPreferencesKey("water_date")
        val theme = stringPreferencesKey("theme")
        val voice = stringPreferencesKey("voice")
        val plus = booleanPreferencesKey("plus")
        val plusPlan = stringPreferencesKey("plus_plan")
        val reminders = booleanPreferencesKey("reminders")
        val interval = intPreferencesKey("interval")
        val voiceReminders = booleanPreferencesKey("voice_reminders")
        val startHour = intPreferencesKey("start_hour")
        val endHour = intPreferencesKey("end_hour")
        val history = stringPreferencesKey("history")
    }

    val state: Flow<AppState> = context.peyVoduDataStore.data.map { p ->
        AppState(
            onboardingDone = p[K.onboarding] ?: false,
            name = p[K.name] ?: "",
            age = p[K.age] ?: 35,
            gender = p[K.gender] ?: "m",
            heightCm = p[K.height] ?: 175,
            weightKg = p[K.weight] ?: 70,
            mode = p[K.mode] ?: "simple",
            glassMl = p[K.glass] ?: 250,
            goalMl = p[K.goal] ?: 1750,
            waterMl = p[K.water] ?: 0,
            waterDate = p[K.waterDate] ?: LocalDate.now().toString(),
            theme = p[K.theme] ?: "purple",
            selectedVoice = p[K.voice] ?: "calm",
            plusEnabled = p[K.plus] ?: false,
            plusPlan = p[K.plusPlan] ?: "1m",
            remindersEnabled = p[K.reminders] ?: true,
            reminderIntervalMin = p[K.interval] ?: 120,
            voiceRemindersEnabled = p[K.voiceReminders] ?: true,
            reminderStartHour = p[K.startHour] ?: 8,
            reminderEndHour = p[K.endHour] ?: 22,
            history = parseHistory(p[K.history].orEmpty())
        )
    }

    suspend fun ensureToday() {
        val p = context.peyVoduDataStore.data.first()
        val today = LocalDate.now().toString()
        val stored = p[K.waterDate]
        if (stored.isNullOrBlank()) {
            context.peyVoduDataStore.edit { it[K.waterDate] = today }
            return
        }
        if (stored != today) {
            val oldMl = p[K.water] ?: 0
            val h = parseHistory(p[K.history].orEmpty()).toMutableList()
            if (oldMl > 0) h.add(DayTotal(stored, oldMl))
            val trimmed = h.takeLast(14)
            context.peyVoduDataStore.edit {
                it[K.water] = 0
                it[K.waterDate] = today
                it[K.history] = encodeHistory(trimmed)
            }
        }
    }

    suspend fun completeOnboarding(
        name: String, age: Int, gender: String, height: Int, weight: Int,
        mode: String, glassMl: Int, voice: String
    ) {
        val goal = defaultGoal(weight)
        context.peyVoduDataStore.edit {
            it[K.onboarding] = true
            it[K.name] = name
            it[K.age] = age
            it[K.gender] = gender
            it[K.height] = height
            it[K.weight] = weight
            it[K.mode] = mode
            it[K.glass] = glassMl
            it[K.goal] = goal
            it[K.voice] = voice
            it[K.waterDate] = LocalDate.now().toString()
        }
    }

    suspend fun addWater(ml: Int) {
        ensureToday()
        context.peyVoduDataStore.edit { p ->
            p[K.water] = (p[K.water] ?: 0) + ml.coerceAtLeast(0)
        }
    }

    suspend fun updateProfile(name: String, age: Int, gender: String, height: Int, weight: Int, goalMl: Int) {
        context.peyVoduDataStore.edit {
            it[K.name] = name
            it[K.age] = age
            it[K.gender] = gender
            it[K.height] = height
            it[K.weight] = weight
            it[K.goal] = goalMl.coerceIn(500, 6000)
        }
    }

    suspend fun setMode(value: String) = context.peyVoduDataStore.edit { it[K.mode] = value }
    suspend fun setGlass(value: Int) = context.peyVoduDataStore.edit { it[K.glass] = value.coerceIn(50, 1500) }
    suspend fun setTheme(value: String) = context.peyVoduDataStore.edit { it[K.theme] = value }
    suspend fun setVoice(value: String) = context.peyVoduDataStore.edit { it[K.voice] = value }
    suspend fun setPlus(plan: String) = context.peyVoduDataStore.edit { it[K.plus] = true; it[K.plusPlan] = plan }
    suspend fun setReminders(value: Boolean) = context.peyVoduDataStore.edit { it[K.reminders] = value }
    suspend fun setVoiceReminders(value: Boolean) = context.peyVoduDataStore.edit { it[K.voiceReminders] = value }
    suspend fun setInterval(value: Int) = context.peyVoduDataStore.edit { it[K.interval] = value.coerceIn(15, 720) }

    suspend fun resetDemo() {
        context.peyVoduDataStore.edit { it.clear() }
    }

    companion object {
        fun defaultGoal(weightKg: Int): Int {
            val raw = (weightKg.coerceAtLeast(1) * 25 / 50f).toInt() * 50
            return raw.coerceIn(1200, 3000)
        }

        private fun parseHistory(raw: String): List<DayTotal> = raw.split(';')
            .mapNotNull { item ->
                val parts = item.split('=')
                if (parts.size == 2) parts[1].toIntOrNull()?.let { DayTotal(parts[0], it) } else null
            }

        private fun encodeHistory(items: List<DayTotal>): String = items.joinToString(";") { "${it.date}=${it.ml}" }
    }
}
