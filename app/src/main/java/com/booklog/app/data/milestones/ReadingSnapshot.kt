package com.booklog.app.data.milestones

data class ReadingSnapshot(
    val totalBooks: Int = 0,
    val wantToRead: Int = 0,
    val reading: Int = 0,
    val finished: Int = 0,
    val pagesFinished: Int = 0,
    val pagesInProgress: Int = 0,
    val ratedBooks: Int = 0,
    val longestFinishedPages: Int = 0,
    val booksScanned: Int = 0,
    val sevenDaySprintAchieved: Boolean = false,
    val bedtimeDaysWithTenPages: Int = 0,
    val distinctFinishedAuthors: Int = 0,
    val pagesReadThisMonth: Int = 0,
    val rewardRedemptions: Int = 0,
    val totalRedeemedCents: Int = 0,
    val totalMinutesRead: Int = 0,
    val earnedCents: Int = 0,
    val availableBalanceCents: Int = 0,
) {
    val totalPages: Int get() = pagesFinished + pagesInProgress

    val readingDollars: Double get() = earnedCents / 100.0

    val availableDollars: Double get() = availableBalanceCents / 100.0

    val readingCents: Int get() = earnedCents

    val centsTowardNextDollar: Int get() = earnedCents % 100

    val pagesUntilNextDollar: Int get() = if (earnedCents == 0) 100 else 100 - centsTowardNextDollar
}