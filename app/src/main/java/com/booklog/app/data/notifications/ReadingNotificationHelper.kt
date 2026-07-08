package com.booklog.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.booklog.app.MainActivity
import com.booklog.app.R

object ReadingNotificationHelper {
    const val CHANNEL_ID = "reading_streak_reminders"
    private const val ENCOURAGEMENT_NOTIFICATION_ID = 2001
    private const val MISS_REMINDER_NOTIFICATION_ID = 2002

    private val encouragementTitles = listOf(
        "📚 Time for an adventure!",
        "🌟 Your book is waiting!",
        "🔥 Keep your streak alive!",
        "📖 Story time calling!",
        "🦸 Be a reading hero today!",
        "🎯 Pages = rewards!",
        "🌈 Discover something new!",
    )

    private val encouragementBodies = listOf(
        "Even a few pages today keeps your reading superpowers strong.",
        "Open BookLog and log a page — every page earns a penny toward rewards!",
        "Kids who read a little every day become amazing readers. You've got this!",
        "Pick up your current book and see where the story takes you next.",
        "Your daily streak is your superpower. Add at least one page today!",
        "Reading builds imagination muscles. Flex them with a quick chapter.",
        "A cozy reading break is the best part of the day. Start now!",
    )

    private val missReminderTitles = listOf(
        "We miss your reading streak!",
        "Your books are lonely 📚",
        "Streak on pause — jump back in!",
        "4 days left to rebuild momentum",
        "Last chance this week!",
    )

    private val missReminderBodies = listOf(
        "You missed yesterday — no worries! Open BookLog and read just one page to restart.",
        "Two days without reading. A quick 5-minute story can bring your streak back!",
        "Three days away from books. Your rewards are waiting — log a page today!",
        "Four missed days. Heroes come back stronger — pick any book and read one page.",
        "Five days without reading. Today is a perfect fresh start. We believe in you!",
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reading streak & reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reading encouragement and missed-day reminders"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showEncouragement(context: Context, streak: Int, messageIndex: Int) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val title = encouragementTitles[messageIndex % encouragementTitles.size]
        val body = if (streak > 0) {
            "${encouragementBodies[messageIndex % encouragementBodies.size]} You're on a $streak-day streak!"
        } else {
            encouragementBodies[messageIndex % encouragementBodies.size]
        }
        post(context, ENCOURAGEMENT_NOTIFICATION_ID, title, body)
    }

    fun showMissReminder(context: Context, missedDays: Int) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val index = (missedDays - 1).coerceIn(0, missReminderTitles.lastIndex)
        post(
            context,
            MISS_REMINDER_NOTIFICATION_ID + index,
            missReminderTitles[index],
            missReminderBodies[index],
        )
    }

    private fun post(context: Context, id: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}