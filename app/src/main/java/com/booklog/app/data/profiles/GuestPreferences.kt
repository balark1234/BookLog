package com.booklog.app.data.profiles

import android.content.Context

class GuestPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isGuestMode(): Boolean = prefs.getBoolean(KEY_GUEST_MODE, false)

    fun setGuestMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUEST_MODE, enabled).apply()
    }

    fun clearGuestMode() {
        prefs.edit().remove(KEY_GUEST_MODE).apply()
    }

    companion object {
        private const val PREFS_NAME = "booklog_guest"
        private const val KEY_GUEST_MODE = "is_guest_mode"
    }
}