package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val name: String,
    val verified: Boolean = false,
    val verificationCode: String = ""
)

@Entity(tableName = "namaz_records")
data class NamazRecord(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val fajr: String = "Missed", // Missed, Prayed, Jamaat
    val dhuhr: String = "Missed",
    val asr: String = "Missed",
    val maghrib: String = "Missed",
    val isisha: String = "Missed" // using isisha instead of isha to avoid any keyword/conflicts
)

@Entity(tableName = "quran_records")
data class QuranRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val type: String, // Pages, Ayat, Ruku
    val amount: Int
)

@Entity(tableName = "prayer_requests")
data class PrayerRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String = "#4CAF50",
    val dateCreated: String, // yyyy-MM-dd
    val reminderTime: String? = null // Format: "HH:mm"
)

@Entity(tableName = "habit_progress")
data class HabitProgress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // yyyy-MM-dd
    val isCompleted: Boolean
)

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val theme: String, // e.g., Focus Forest, Midnight Oil, Library Silence
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String // yyyy-MM-dd
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String, // yyyy-MM-dd
    val colorHex: String = "#FF9800",
    val isReminder: Boolean = false,
    val time: String = "12:00"
)

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val date: String, // yyyy-MM-dd
    val mood: Int, // 1 to 5
    val isLocked: Boolean = false,
    val pinCode: String = ""
)

@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val sleepHours: Float
)
