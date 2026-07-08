package com.booklog.app.data.streak

import com.booklog.app.data.local.ReadingDayLog
import com.booklog.app.data.repository.RewardRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReadingStreakInfo(
    val currentStreak: Int,
    val readToday: Boolean,
    val consecutiveMissedDays: Int,
    val pagesToday: Int,
)

object ReadingStreakCalculator {
    fun compute(logs: List<ReadingDayLog>): ReadingStreakInfo {
        val pagesByDay = logs.groupBy { it.dayKey }.mapValues { (_, entries) ->
            entries.sumOf { it.pagesLogged }
        }
        val readingDays = pagesByDay.filter { it.value > 0 }.keys
        val today = RewardRepository.todayKey()
        val readToday = (pagesByDay[today] ?: 0) > 0
        val pagesToday = pagesByDay[today] ?: 0

        val streak = calculateCurrentStreak(readingDays, today)
        val missed = calculateConsecutiveMissedDays(readingDays, today)

        return ReadingStreakInfo(
            currentStreak = streak,
            readToday = readToday,
            consecutiveMissedDays = missed,
            pagesToday = pagesToday,
        )
    }

    private fun calculateCurrentStreak(readingDays: Set<String>, today: String): Int {
        if (readingDays.isEmpty()) return 0
        val startKey = when {
            today in readingDays -> today
            previousDayKey(today) in readingDays -> previousDayKey(today)
            else -> return 0
        }
        var streak = 0
        var cursor = startKey
        while (cursor in readingDays) {
            streak++
            cursor = previousDayKey(cursor)
        }
        return streak
    }

    private fun calculateConsecutiveMissedDays(readingDays: Set<String>, today: String): Int {
        if (today in readingDays) return 0
        var missed = 0
        var cursor = previousDayKey(today)
        while (missed < 5) {
            if (cursor in readingDays) break
            missed++
            cursor = previousDayKey(cursor)
        }
        return missed
    }

    private fun previousDayKey(dayKey: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = format.parse(dayKey) ?: return dayKey
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return format.format(cal.time)
    }
}