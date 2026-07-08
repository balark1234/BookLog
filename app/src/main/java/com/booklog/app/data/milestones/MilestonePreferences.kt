package com.booklog.app.data.milestones

import android.content.Context

class MilestonePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_milestones", Context.MODE_PRIVATE)

    fun getBooksScanned(): Int = prefs.getInt(KEY_SCAN_COUNT, 0)

    fun recordBookScanned() {
        prefs.edit().putInt(KEY_SCAN_COUNT, getBooksScanned() + 1).apply()
    }

    companion object {
        private const val KEY_SCAN_COUNT = "books_scanned"
    }
}