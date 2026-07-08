package com.booklog.app.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.booklog.app.BookLogApplication
import com.booklog.app.data.profiles.ActiveKidPreferences
import com.booklog.app.data.repository.RewardRepository
import com.booklog.app.data.streak.ReadingStreakCalculator
import kotlinx.coroutines.flow.first

class ReadingReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BookLogApplication ?: return Result.failure()
        val prefs = ReadingNotificationPreferences(applicationContext)
        val today = RewardRepository.todayKey()
        val activeKidId = ActiveKidPreferences(applicationContext).getActiveKidId()
        val logs = app.rewardRepository.observeReadingLogs(activeKidId).first()
        val streakInfo = ReadingStreakCalculator.compute(logs)

        if (streakInfo.readToday) {
            if (prefs.getLastEncouragementDay() != today) {
                val nextIndex = (prefs.getLastEncouragementIndex() + 1) % 7
                ReadingNotificationHelper.showEncouragement(
                    applicationContext,
                    streakInfo.currentStreak,
                    nextIndex,
                )
                prefs.markEncouragementSent(today, nextIndex)
            }
        } else if (streakInfo.consecutiveMissedDays in 1..5) {
            if (prefs.getLastMissReminderDay() != today) {
                ReadingNotificationHelper.showMissReminder(
                    applicationContext,
                    streakInfo.consecutiveMissedDays,
                )
                prefs.markMissReminderSent(today)
            }
        }

        return Result.success()
    }
}