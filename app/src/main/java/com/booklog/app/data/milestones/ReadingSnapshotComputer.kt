package com.booklog.app.data.milestones

import com.booklog.app.data.local.Book
import com.booklog.app.data.local.ReadingDayLog
import com.booklog.app.data.local.ReadingStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReadingSnapshotComputer {
    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    fun compute(
        books: List<Book>,
        readingLogs: List<ReadingDayLog>,
        booksScanned: Int,
        totalRedeemedCents: Int,
        rewardRedemptions: Int,
    ): ReadingSnapshot {
        val finishedBooks = books.filter { it.status == ReadingStatus.FINISHED }
        val monthPrefix = currentMonthPrefix()

        val pagesByDay = readingLogs.groupBy { it.dayKey }.mapValues { (_, logs) ->
            logs.sumOf { it.pagesLogged }
        }
        val bedtimeDays = pagesByDay.count { it.value >= 10 }
        val pagesThisMonth = readingLogs
            .filter { it.dayKey.startsWith(monthPrefix) }
            .sumOf { it.pagesLogged }

        val distinctAuthors = finishedBooks
            .map { it.author.trim().lowercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
            .size

        val sevenDaySprint = finishedBooks.any { book ->
            val started = book.dateStarted ?: return@any false
            val finished = book.dateFinished ?: return@any false
            finished - started <= SEVEN_DAYS_MS
        }

        return ReadingSnapshot(
            totalBooks = books.size,
            wantToRead = books.count { it.status == ReadingStatus.WANT_TO_READ },
            reading = books.count { it.status == ReadingStatus.READING },
            finished = finishedBooks.size,
            pagesFinished = finishedBooks.sumOf { it.pageCount ?: 0 },
            pagesInProgress = books
                .filter { it.status == ReadingStatus.READING }
                .sumOf { it.currentPage ?: 0 },
            ratedBooks = books.count { (it.rating ?: 0f) > 0f },
            longestFinishedPages = finishedBooks.maxOfOrNull { it.pageCount ?: 0 } ?: 0,
            booksScanned = booksScanned,
            sevenDaySprintAchieved = sevenDaySprint,
            bedtimeDaysWithTenPages = bedtimeDays,
            distinctFinishedAuthors = distinctAuthors,
            pagesReadThisMonth = pagesThisMonth,
            rewardRedemptions = rewardRedemptions,
            totalRedeemedCents = totalRedeemedCents,
        )
    }

    private fun currentMonthPrefix(): String {
        val cal = Calendar.getInstance()
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }
}