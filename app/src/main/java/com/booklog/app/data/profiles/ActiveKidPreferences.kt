package com.booklog.app.data.profiles

import android.content.Context

class ActiveKidPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_active_kid", Context.MODE_PRIVATE)

    /** Returns active kid id, or null for parent / no kid filter. */
    fun getActiveKidId(): Long? {
        return when (val stored = prefs.getLong(KEY_ACTIVE_KID_ID, NOT_SET)) {
            NOT_SET, PARENT_SELECTED -> null
            else -> stored
        }
    }

    fun isUnset(): Boolean = prefs.getLong(KEY_ACTIVE_KID_ID, NOT_SET) == NOT_SET

    fun setActiveKidId(id: Long?) {
        val stored = when (id) {
            null -> PARENT_SELECTED
            else -> id
        }
        prefs.edit()
            .putLong(KEY_ACTIVE_KID_ID, stored)
            .apply()
    }

    companion object {
        private const val KEY_ACTIVE_KID_ID = "active_kid_id"
        private const val NOT_SET = -1L
        private const val PARENT_SELECTED = 0L
    }
}