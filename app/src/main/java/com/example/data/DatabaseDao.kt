package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DatabaseDao {

    // --- User Accounts ---
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccount): Long

    @Query("DELETE FROM user_accounts WHERE email = :email")
    suspend fun deleteAccountByEmail(email: String)


    // --- Namaz Records ---
    @Query("SELECT * FROM namaz_records WHERE date = :date LIMIT 1")
    fun getNamazRecordForDate(date: String): Flow<NamazRecord?>

    @Query("SELECT * FROM namaz_records ORDER BY date DESC LIMIT 30")
    fun getRecentNamazRecords(): Flow<List<NamazRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNamazRecord(record: NamazRecord)


    // --- Quran Records ---
    @Query("SELECT * FROM quran_records ORDER BY date DESC")
    fun getAllQuranRecords(): Flow<List<QuranRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranRecord(record: QuranRecord)

    @Query("DELETE FROM quran_records WHERE id = :id")
    suspend fun deleteQuranRecord(id: Int)


    // --- Prayer Requests ---
    @Query("SELECT * FROM prayer_requests ORDER BY dateCreated DESC")
    fun getAllPrayerRequests(): Flow<List<PrayerRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRequest(request: PrayerRequest)

    @Query("DELETE FROM prayer_requests WHERE id = :id")
    suspend fun deletePrayerRequest(id: Int)


    // --- Habits & Progress ---
    @Query("SELECT * FROM habits ORDER BY id DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Int)

    @Query("SELECT * FROM habit_progress WHERE habitId = :habitId ORDER BY date DESC")
    fun getProgressForHabit(habitId: Int): Flow<List<HabitProgress>>

    @Query("SELECT * FROM habit_progress ORDER BY date DESC")
    fun getAllHabitProgress(): Flow<List<HabitProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitProgress(progress: HabitProgress)

    @Query("DELETE FROM habit_progress WHERE habitId = :habitId AND date = :date")
    suspend fun deleteHabitProgress(habitId: Int, date: String)


    // --- Study Sessions ---
    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession)


    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events ORDER BY date ASC, time ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: Int)


    // --- Diary Entries ---
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date")
    fun getDiaryEntriesByDate(date: String): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteDiaryEntry(id: Int)


    // --- Chat History ---
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getChatHistory(): Flow<List<ChatHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatHistory)

    @Query("DELETE FROM chat_history")
    suspend fun clearChatHistory()

    // --- Sleep Records ---
    @Query("SELECT * FROM sleep_records ORDER BY date DESC")
    fun getAllSleepRecords(): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecord)
}
