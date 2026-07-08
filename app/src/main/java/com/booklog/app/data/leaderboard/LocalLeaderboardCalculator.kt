package com.booklog.app.data.leaderboard

import com.booklog.app.data.local.Book
import com.booklog.app.data.local.CompletedBook
import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.local.ReadingDayLog
import com.booklog.app.data.streak.ReadingStreakCalculator

data class LocalLeaderboardEntry(
    val kidId: Long,
    val displayName: String,
    val emoji: String,
    val minutesRead: Int,
    val pagesRead: Int,
    val booksCompleted: Int,
    val currentStreak: Int,
    val rewardCents: Int,
    val rank: Int = 0,
)

object LocalLeaderboardCalculator {
    fun compute(
        kids: List<KidProfile>,
        completionsByKid: Map<Long, List<CompletedBook>>,
        booksByKid: Map<Long, List<Book>>,
        logsByKid: Map<Long, List<ReadingDayLog>>,
        balanceByKid: Map<Long, Int>,
    ): List<LocalLeaderboardEntry> {
        val entries = kids.map { kid ->
            val completions = completionsByKid[kid.id].orEmpty()
            val books = booksByKid[kid.id].orEmpty()
            val logs = logsByKid[kid.id].orEmpty()
            val streak = ReadingStreakCalculator.compute(logs).currentStreak
            LocalLeaderboardEntry(
                kidId = kid.id,
                displayName = kid.firstName,
                emoji = kid.emoji,
                minutesRead = completions.sumOf { it.minutesRead },
                pagesRead = completions.sumOf { it.pageCount ?: 0 },
                booksCompleted = completions.size,
                currentStreak = streak,
                rewardCents = balanceByKid[kid.id] ?: completions.sumOf { it.totalRewardCents },
            )
        }
        return entries
            .sortedWith(
                compareByDescending<LocalLeaderboardEntry> { it.minutesRead }
                    .thenByDescending { it.pagesRead }
                    .thenByDescending { it.booksCompleted },
            )
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }
}