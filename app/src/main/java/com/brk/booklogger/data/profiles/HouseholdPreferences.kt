package com.brk.booklogger.data.profiles

import android.content.Context

/** Local cache of household link (authoritative value also lives on users/{uid} in Firestore). */
class HouseholdPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("booklog_household", Context.MODE_PRIVATE)

    fun getHouseholdId(): String? = prefs.getString(KEY_HOUSEHOLD_ID, null)

    fun setHouseholdId(id: String?) {
        prefs.edit().apply {
            if (id == null) remove(KEY_HOUSEHOLD_ID) else putString(KEY_HOUSEHOLD_ID, id)
            apply()
        }
    }

    fun getInviteCode(): String? = prefs.getString(KEY_INVITE_CODE, null)

    fun setInviteCode(code: String?) {
        prefs.edit().apply {
            if (code == null) remove(KEY_INVITE_CODE) else putString(KEY_INVITE_CODE, code)
            apply()
        }
    }

    companion object {
        private const val KEY_HOUSEHOLD_ID = "household_id"
        private const val KEY_INVITE_CODE = "invite_code"
    }
}
