package com.thxrealx7.peyvodu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thxrealx7.peyvodu.data.AppState
import com.thxrealx7.peyvodu.data.SettingsRepository
import com.thxrealx7.peyvodu.reminder.NotificationHelper
import com.thxrealx7.peyvodu.reminder.ReminderScheduler
import com.thxrealx7.peyvodu.reminder.VoicePlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeyVoduViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SettingsRepository(application)

    val state: StateFlow<AppState> = repo.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppState()
    )

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds
    private var cooldownUntil = 0L

    init {
        viewModelScope.launch { repo.ensureToday() }
        viewModelScope.launch {
            state.map { Triple(it.onboardingDone && it.remindersEnabled, it.reminderIntervalMin, it.remindersEnabled) }
                .distinctUntilChanged()
                .collect { (ready, interval, enabled) ->
                    ReminderScheduler.apply(getApplication(), ready && enabled, interval)
                }
        }
    }

    fun completeOnboarding(
        name: String,
        age: Int,
        gender: String,
        height: Int,
        weight: Int,
        mode: String,
        glassMl: Int,
        voice: String
    ) {
        viewModelScope.launch {
            repo.completeOnboarding(name, age, gender, height, weight, mode, glassMl, voice)
        }
    }

    fun addWater(ml: Int) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now < cooldownUntil) return
        cooldownUntil = now + 10_000L
        startCooldownTicker()

        val before = state.value
        val newTotal = before.waterMl + ml.coerceAtLeast(0)
        val newProgress = newTotal.toFloat() / before.goalMl.coerceAtLeast(1)

        viewModelScope.launch {
            repo.addWater(ml)
            if (before.voiceRemindersEnabled) {
                VoicePlayer.play(
                    getApplication(),
                    VoicePlayer.feedbackResource(newProgress),
                    before.selectedVoice
                )
            }
        }
    }

    private fun startCooldownTicker() {
        viewModelScope.launch {
            while (true) {
                val left = ((cooldownUntil - android.os.SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                _cooldownSeconds.value = left.coerceAtLeast(0)
                if (left <= 0) break
                delay(250)
            }
        }
    }

    fun setMode(mode: String) = viewModelScope.launch { repo.setMode(mode) }
    fun setTheme(theme: String) = viewModelScope.launch { repo.setTheme(theme) }
    fun setGlass(ml: Int) = viewModelScope.launch { repo.setGlass(ml) }
    fun setVoice(voice: String) = viewModelScope.launch { repo.setVoice(voice) }
    fun setReminders(enabled: Boolean) = viewModelScope.launch { repo.setReminders(enabled) }
    fun setVoiceReminders(enabled: Boolean) = viewModelScope.launch { repo.setVoiceReminders(enabled) }
    fun setInterval(minutes: Int) = viewModelScope.launch { repo.setInterval(minutes) }
    fun activatePlus(plan: String) = viewModelScope.launch { repo.setPlus(plan) }

    fun updateProfile(name: String, age: Int, gender: String, height: Int, weight: Int, goalMl: Int) =
        viewModelScope.launch { repo.updateProfile(name, age, gender, height, weight, goalMl) }

    fun previewVoice(voice: String = state.value.selectedVoice) {
        viewModelScope.launch {
            VoicePlayer.play(getApplication(), com.thxrealx7.peyvodu.R.raw.calm_01, voice)
        }
    }

    fun testNotification() {
        NotificationHelper.show(getApplication(), "Пора выпить немного воды")
    }

    fun resetDemo() = viewModelScope.launch { repo.resetDemo() }
}
