package com.booklog.app.data.audio

import android.content.Context

class MilestoneCelebrationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_milestone_celebrations", Context.MODE_PRIVATE)

    fun isFirstCelebrationCheck(): Boolean =
        !prefs.contains(KEY_CELEBRATED)

    fun findNewlyUnlocked(currentUnlockedIds: Set<String>): List<String> {
        val celebrated = prefs.getStringSet(KEY_CELEBRATED, emptySet()).orEmpty()
        return currentUnlockedIds.filter { it !in celebrated }
    }

    fun syncCelebratedIds(unlockedIds: Set<String>) {
        prefs.edit().putStringSet(KEY_CELEBRATED, unlockedIds).apply()
    }

    companion object {
        private const val KEY_CELEBRATED = "celebrated_milestone_ids"
    }
}