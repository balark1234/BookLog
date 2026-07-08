package com.booklog.app.data.profiles

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object KidAgeCalculator {
    fun localDateToMillis(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun millisToLocalParts(millis: Long): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = normalizeStoredMillis(millis) }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Corrects legacy UTC-midnight values from Material DatePicker. */
    fun normalizeStoredMillis(millis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
        return localDateToMillis(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun ageLabel(dateOfBirthMillis: Long?): String {
        if (dateOfBirthMillis == null) return "Age not set"
        val (year, month, day) = millisToLocalParts(dateOfBirthMillis)
        val today = Calendar.getInstance()
        var years = today.get(Calendar.YEAR) - year
        val todayMonthDay = today.get(Calendar.MONTH) * 100 + today.get(Calendar.DAY_OF_MONTH)
        val birthMonthDay = month * 100 + day
        if (todayMonthDay < birthMonthDay) years--
        years = years.coerceAtLeast(0)
        return if (years == 1) "1 year old" else "$years years old"
    }

    fun birthdateLabel(dateOfBirthMillis: Long?): String {
        if (dateOfBirthMillis == null) return "Not set"
        val (year, month, day) = millisToLocalParts(dateOfBirthMillis)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return String.format(
            Locale.getDefault(),
            "%tb %d, %d",
            cal,
            day,
            year,
        )
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val monthNames: List<String> = Calendar.getInstance().let { cal ->
        (0..11).map { month ->
            cal.set(Calendar.MONTH, month)
            cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        }
    }
}