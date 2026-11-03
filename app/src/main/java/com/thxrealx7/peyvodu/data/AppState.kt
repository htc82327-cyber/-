package com.thxrealx7.peyvodu.data

data class DayTotal(val date: String, val ml: Int)

data class AppState(
    val onboardingDone: Boolean = false,
    val name: String = "",
    val age: Int = 35,
    val gender: String = "m",
    val heightCm: Int = 175,
    val weightKg: Int = 70,
    val mode: String = "simple",
    val glassMl: Int = 250,
    val goalMl: Int = 1750,
    val waterMl: Int = 0,
    val waterDate: String = "",
    val theme: String = "purple",
    val selectedVoice: String = "calm",
    val plusEnabled: Boolean = false,
    val plusPlan: String = "1m",
    val remindersEnabled: Boolean = true,
    val reminderIntervalMin: Int = 120,
    val voiceRemindersEnabled: Boolean = true,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 22,
    val history: List<DayTotal> = emptyList()
) {
    val isSimple: Boolean get() = mode == "simple"
    val glassesDone: Int get() = if (glassMl <= 0) 0 else waterMl / glassMl
    val glassesGoal: Int get() = kotlin.math.max(1, kotlin.math.round(goalMl.toFloat() / glassMl.coerceAtLeast(1)).toInt())
    val progress: Float get() = (waterMl.toFloat() / goalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
}
