package com.booklog.app.data.notifications

import android.content.Context

class ReadingNotificationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_reading_notifications", Context.MODE_PRIVATE)

    fun getLastEncouragementDay(): String? = prefs.getString(KEY_ENCOURAGEMENT_DAY, null)

    fun getLastMissReminderDay(): String? = prefs.getString(KEY_MISS_REMINDER_DAY, null)

    fun getLastEncouragementIndex(): Int = prefs.getInt(KEY_ENCOURAGEMENT_INDEX, 0)

    fun markEncouragementSent(dayKey: String, messageIndex: Int) {
        prefs.edit()
            .putString(KEY_ENCOURAGEMENT_DAY, dayKey)
            .putInt(KEY_ENCOURAGEMENT_INDEX, messageIndex)
            .remove(KEY_MISS_REMINDER_DAY)
            .apply()
    }

    fun markMissReminderSent(dayKey: String) {
        prefs.edit()
            .putString(KEY_MISS_REMINDER_DAY, dayKey)
            .apply()
    }

    companion object {
        private const val KEY_ENCOURAGEMENT_DAY = "last_encouragement_day"
        private const val KEY_MISS_REMINDER_DAY = "last_miss_reminder_day"
        private const val KEY_ENCOURAGEMENT_INDEX = "last_encouragement_index"
    }
}