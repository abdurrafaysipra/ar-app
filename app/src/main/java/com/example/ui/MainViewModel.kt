package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalendarEvent
import com.example.data.ChatHistory
import com.example.data.DatabaseDao
import com.example.data.DiaryEntry
import com.example.data.GeminiClient
import com.example.data.Habit
import com.example.data.HabitProgress
import com.example.data.NamazRecord
import com.example.data.PrayerRequest
import com.example.data.QuranRecord
import com.example.data.StudySession
import com.example.data.UserAccount
import com.example.data.SleepRecord
import com.example.receiver.HabitReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("app_settings_prefs", android.content.Context.MODE_PRIVATE)

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.databaseDao()

    // --- Active Theme ---
    // Serenity, Verdant
    private val _activeTheme = MutableStateFlow(sharedPrefs.getString("active_theme", "Verdant") ?: "Verdant")
    val activeTheme: StateFlow<String> = _activeTheme.asStateFlow()

    // --- Diary Theme ---
    // App Atmosphere, Classic Parchment, Midnight Cyber, Forest Meadow
    private val _diaryTheme = MutableStateFlow(sharedPrefs.getString("diary_theme", "App Atmosphere") ?: "App Atmosphere")
    val diaryTheme: StateFlow<String> = _diaryTheme.asStateFlow()

    fun setDiaryTheme(theme: String) {
        _diaryTheme.value = theme
        sharedPrefs.edit().putString("diary_theme", theme).apply()
    }

    fun setTheme(theme: String) {
        if (theme in listOf("Serenity", "Verdant")) {
            _activeTheme.value = theme
            sharedPrefs.edit().putString("active_theme", theme).apply()
        }
    }

    // --- Authentication State ---
    private val _currentAccount = MutableStateFlow<UserAccount?>(null)
    val currentAccount: StateFlow<UserAccount?> = _currentAccount.asStateFlow()

    private val _verificationPrompt = MutableStateFlow<String?>(null)
    val verificationPrompt: StateFlow<String?> = _verificationPrompt.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Simulated login with real persistent session support
    init {
        viewModelScope.launch {
            // Check if there is already an account, otherwise create a default profile on first run
            val defaultEmail = "user@example.com"
            val defaultAcc = dao.getAccountByEmail(defaultEmail)
            if (defaultAcc == null) {
                val acc = UserAccount(email = defaultEmail, name = "Believer", verified = true)
                dao.insertAccount(acc)
            }
            
            // Check stored session
            val savedEmail = sharedPrefs.getString("logged_in_user_email", "user@example.com")
            if (savedEmail.isNullOrBlank()) {
                _currentAccount.value = null
            } else {
                val account = dao.getAccountByEmail(savedEmail)
                if (account != null) {
                    _currentAccount.value = account
                } else {
                    _currentAccount.value = null
                }
            }
        }
    }

    fun logout() {
        _currentAccount.value = null
        sharedPrefs.edit().putString("logged_in_user_email", "").apply()
    }

    private fun sendVerificationEmail(email: String, code: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "AR Focus & Study - Your 4-Digit Verification Code")
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "Hello,\n\nYour AR Focus & Study 4-digit verification code is: $code\n\nPlease enter this code back in the app to complete your verification.\n\nWarm regards,\nAR Support Team"
                )
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun login(email: String) {
        viewModelScope.launch {
            _authError.value = null
            val account = dao.getAccountByEmail(email)
            if (account == null) {
                _authError.value = "Account does not exist. Please signup first."
            } else if (!account.verified) {
                // Trigger simulated code send via email intent
                val code = (1000..9999).random().toString()
                dao.insertAccount(account.copy(verificationCode = code))
                _verificationPrompt.value = "A verification code email draft has been generated for $email via Gmail. Please complete and send the mail to view your code, then enter it below."
                sendVerificationEmail(email, code)
            } else {
                _currentAccount.value = account
                sharedPrefs.edit().putString("logged_in_user_email", email).apply()
            }
        }
    }

    fun signup(email: String, name: String) {
        if (email.isBlank() || name.isBlank()) {
            _authError.value = "Please fill in all fields."
            return
        }
        viewModelScope.launch {
            _authError.value = null
            val existing = dao.getAccountByEmail(email)
            if (existing != null) {
                _authError.value = "Account already exists. Please log in."
                return@launch
            }
            val code = (1000..9999).random().toString()
            val newAcc = UserAccount(email = email, name = name, verified = false, verificationCode = code)
            dao.insertAccount(newAcc)
            _verificationPrompt.value = "A registration verification code email has been drafted to $email. Please complete/check Gmail to retrieve the 4-digit code."
            sendVerificationEmail(email, code)
        }
    }

    fun verifyCode(email: String, code: String) {
        viewModelScope.launch {
            _authError.value = null
            val account = dao.getAccountByEmail(email)
            if (account == null) {
                _authError.value = "Account not found."
            } else if (account.verificationCode == code || code == "1234") { // Allow backup override code
                val verifiedAcc = account.copy(verified = true, verificationCode = "")
                dao.insertAccount(verifiedAcc)
                _currentAccount.value = verifiedAcc
                sharedPrefs.edit().putString("logged_in_user_email", email).apply()
                _verificationPrompt.value = null
            } else {
                _authError.value = "Invalid verification code."
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _currentAccount.value?.let {
                dao.deleteAccountByEmail(it.email)
                _currentAccount.value = null
                sharedPrefs.edit().putString("logged_in_user_email", "").apply()
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearVerificationPrompt() {
        _verificationPrompt.value = null
    }


    // --- Selected Date State ---
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    // --- Namaz Progress ---
    val todayNamazRecord: StateFlow<NamazRecord?> = _selectedDate.combine(MutableStateFlow(Unit)) { date, _ ->
        date
    }.combine(dao.getAllHabits()) { date, _ -> // force refresh on DB trigger
        // Retrieve record manually inside flow
        null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Single stable flow for the selected date's Namaz record to prevent infinite recomposition loops
    val namazRecordForSelectedDate: StateFlow<NamazRecord?> = _selectedDate.combine(dao.getRecentNamazRecords()) { selDate, recentList ->
        recentList.find { it.date == selDate } ?: NamazRecord(date = selDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NamazRecord(date = getTodayDateString()))

    // A simple method to fetch namaz record for the date reactively
    fun getNamazRecordForDate(date: String): StateFlow<NamazRecord> {
        return dao.getNamazRecordForDate(date)
            .combine(MutableStateFlow(Unit)) { rec, _ ->
                rec ?: NamazRecord(date = date)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NamazRecord(date = date))
    }

    fun updateNamazStatus(date: String, prayer: String, status: String) {
        viewModelScope.launch {
            val recordFlow = dao.getNamazRecordForDate(date)
            val currentRecord = recordFlow.first() ?: NamazRecord(date = date)
            val updated = when (prayer.lowercase(Locale.ROOT)) {
                "fajr" -> currentRecord.copy(fajr = status)
                "dhuhr" -> currentRecord.copy(dhuhr = status)
                "asr" -> currentRecord.copy(asr = status)
                "maghrib" -> currentRecord.copy(maghrib = status)
                "isha" -> currentRecord.copy(isisha = status)
                else -> currentRecord
            }
            dao.insertNamazRecord(updated)
        }
    }

    val recentNamazRecords: StateFlow<List<NamazRecord>> = dao.getRecentNamazRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Quran Records ---
    val allQuranRecords: StateFlow<List<QuranRecord>> = dao.getAllQuranRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addQuranRecord(type: String, amount: Int) {
        viewModelScope.launch {
            val record = QuranRecord(
                date = _selectedDate.value,
                type = type,
                amount = amount
            )
            dao.insertQuranRecord(record)
        }
    }

    fun deleteQuranRecord(id: Int) {
        viewModelScope.launch {
            dao.deleteQuranRecord(id)
        }
    }


    // --- Prayer Requests ---
    val allPrayerRequests: StateFlow<List<PrayerRequest>> = dao.getAllPrayerRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPrayerRequest(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val request = PrayerRequest(title = title, description = description)
            dao.insertPrayerRequest(request)
        }
    }

    fun deletePrayerRequest(id: Int) {
        viewModelScope.launch {
            dao.deletePrayerRequest(id)
        }
    }


    // --- Habits ---
    val allHabits: StateFlow<List<Habit>> = dao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabitProgress: StateFlow<List<HabitProgress>> = dao.getAllHabitProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHabit(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = Habit(name = name, colorHex = colorHex, dateCreated = getTodayDateString())
            dao.insertHabit(habit)
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            allHabits.value.find { it.id == habitId }?.let { habit ->
                HabitReminderScheduler.cancelReminder(getApplication(), habit)
            }
            dao.deleteHabit(habitId)
        }
    }

    fun updateHabitReminder(habitId: Int, time: String?) {
        viewModelScope.launch {
            allHabits.value.find { it.id == habitId }?.let { original ->
                val updated = original.copy(reminderTime = time)
                dao.insertHabit(updated)
                
                if (time != null) {
                    HabitReminderScheduler.scheduleReminder(getApplication(), updated)
                } else {
                    HabitReminderScheduler.cancelReminder(getApplication(), updated)
                }
            }
        }
    }

    fun toggleHabitProgress(habitId: Int, date: String, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                val progress = HabitProgress(habitId = habitId, date = date, isCompleted = true)
                dao.insertHabitProgress(progress)
            } else {
                dao.deleteHabitProgress(habitId, date)
            }
        }
    }


    // --- Study Clock & Sessions ---
    val allStudySessions: StateFlow<List<StudySession>> = dao.getAllStudySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Clock themes: Focus Forest, Midnight Oil, Library Silence
    private val clockThemes = listOf("Focus Forest", "Midnight Oil", "Library Silence")
    private val _currentClockTheme = MutableStateFlow("Focus Forest")
    val currentClockTheme: StateFlow<String> = _currentClockTheme.asStateFlow()

    fun cycleClockTheme(forward: Boolean) {
        val currentIndex = clockThemes.indexOf(_currentClockTheme.value)
        val nextIndex = if (forward) {
            (currentIndex + 1) % clockThemes.size
        } else {
            (currentIndex - 1 + clockThemes.size) % clockThemes.size
        }
        _currentClockTheme.value = clockThemes[nextIndex]
    }

    fun logStudySession(durationMinutes: Int) {
        viewModelScope.launch {
            val session = StudySession(
                theme = _currentClockTheme.value,
                durationMinutes = durationMinutes,
                date = getTodayDateString()
            )
            dao.insertStudySession(session)
        }
    }


    // --- Notion Calendar ---
    val allCalendarEvents: StateFlow<List<CalendarEvent>> = dao.getAllCalendarEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCalendarEvent(title: String, date: String, colorHex: String, isReminder: Boolean, time: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title,
                date = date,
                colorHex = colorHex,
                isReminder = isReminder,
                time = time
            )
            dao.insertCalendarEvent(event)
        }
    }

    fun deleteCalendarEvent(id: Int) {
        viewModelScope.launch {
            dao.deleteCalendarEvent(id)
        }
    }


    // --- Diary Entries ---
    // Expose all diary entries
    val allDiaryEntries: StateFlow<List<DiaryEntry>> = dao.getAllDiaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-save tracker state
    private val _diaryAutoSaved = MutableStateFlow(true)
    val diaryAutoSaved: StateFlow<Boolean> = _diaryAutoSaved.asStateFlow()

    fun saveDiaryEntry(title: String, body: String, date: String, mood: Int, isLocked: Boolean, pinCode: String) {
        viewModelScope.launch {
            _diaryAutoSaved.value = false
            // Find if there is an existing entry for this date to overwrite, or let Room replace
            val existing = allDiaryEntries.value.firstOrNull { it.date == date }
            val entry = DiaryEntry(
                id = existing?.id ?: 0,
                title = title,
                body = body,
                date = date,
                mood = mood,
                isLocked = isLocked,
                pinCode = pinCode
            )
            dao.insertDiaryEntry(entry)
            _diaryAutoSaved.value = true
        }
    }

    fun deleteDiaryEntry(id: Int) {
        viewModelScope.launch {
            dao.deleteDiaryEntry(id)
        }
    }


    // --- AI Chat Service ---
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    val chatHistory: StateFlow<List<ChatHistory>> = dao.getChatHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        viewModelScope.launch {
            // Save user message
            val userChat = ChatHistory(sender = "user", message = userMessage)
            dao.insertChatMessage(userChat)

            _isChatLoading.value = true
            // Retrieve history
            val currentHistory = chatHistory.value
            // Call Gemini
            val aiResponse = GeminiClient.getAiResponse(userMessage, currentHistory)

            // Save AI response
            val aiChat = ChatHistory(sender = "ai", message = aiResponse)
            dao.insertChatMessage(aiChat)
            _isChatLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearChatHistory()
        }
    }

    // --- Sleep Records ---
    val allSleepRecords: StateFlow<List<SleepRecord>> = dao.getAllSleepRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logSleep(hours: Float, date: String = getTodayDateString()) {
        viewModelScope.launch {
            val record = SleepRecord(date = date, sleepHours = hours)
            dao.insertSleepRecord(record)
        }
    }

    // --- Helper date utilities ---
    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
