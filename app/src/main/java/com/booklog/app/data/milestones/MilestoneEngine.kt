package com.booklog.app.data.milestones

object MilestoneEngine {
    fun compute(snapshot: ReadingSnapshot): List<Milestone> {
        val dollars = snapshot.readingDollars
        val dollarDisplay = String.format("$%.2f", dollars)

        return listOf(
            Milestone(
                id = "reading_dollars",
                title = "Reading Rewards",
                description = "Earn \$1 for every 100 pages you read — every page counts as a penny!",
                emoji = "💰",
                category = MilestoneCategory.REWARDS,
                target = 100,
                current = snapshot.centsTowardNextDollar,
                isUnlocked = snapshot.totalPages >= 100,
                isFeatured = true,
                displayValue = if (snapshot.totalPages == 0) {
                    "\$0.00 earned · 100 pages to \$1"
                } else {
                    "$dollarDisplay earned · ${snapshot.pagesUntilNextDollar} pages to next \$1"
                },
            ),
            milestone("first_finish", "First Finish!", "Complete your very first book", "🎉",
                MilestoneCategory.BOOKS, 1, snapshot.finished),
            milestone("finish_3", "Hat Trick", "Finish 3 books — you're on a roll!", "📚",
                MilestoneCategory.BOOKS, 3, snapshot.finished),
            milestone("finish_5", "High Five", "Finish 5 books and high-five yourself", "✋",
                MilestoneCategory.BOOKS, 5, snapshot.finished),
            milestone("finish_10", "Double Digits", "Finish 10 books — true bookworm status", "🐛",
                MilestoneCategory.BOOKS, 10, snapshot.finished),
            milestone("pages_50", "Page Explorer", "Read 50 pages total", "🗺️",
                MilestoneCategory.PAGES, 50, snapshot.totalPages),
            milestone("pages_100", "Century Club", "Hit 100 pages — that's \$1 in rewards!", "💯",
                MilestoneCategory.PAGES, 100, snapshot.totalPages),
            milestone("pages_500", "Page Mountain", "Climb to 500 pages read", "⛰️",
                MilestoneCategory.PAGES, 500, snapshot.totalPages),
            milestone("pages_1000", "Legendary Reader", "Read 1,000 pages — incredible!", "👑",
                MilestoneCategory.PAGES, 1000, snapshot.totalPages),
            milestone("shelf_3", "Shelf Starter", "Add 3 books to your library", "📥",
                MilestoneCategory.FUN, 3, snapshot.totalBooks),
            milestone("scan_3", "Barcode Hunter", "Scan 3 books with your camera", "📷",
                MilestoneCategory.FUN, 3, snapshot.booksScanned),
            milestone("rate_1", "First Star", "Rate your first book", "⭐",
                MilestoneCategory.FUN, 1, snapshot.ratedBooks),
            milestone("reading_now", "Currently Curious", "Have a book marked as Reading", "🔖",
                MilestoneCategory.FUN, 1, snapshot.reading),
            milestone("big_book", "Big Book Boss", "Finish a book with 250+ pages", "🏔️",
                MilestoneCategory.FUN, 1, if (snapshot.longestFinishedPages >= 250) 1 else 0),
            milestone("sprint_7day", "7-Day Sprint", "Finish any book within 7 days of starting", "📅",
                MilestoneCategory.FUN, 1, if (snapshot.sevenDaySprintAchieved) 1 else 0),
            milestone("bedtime_reader", "Bedtime Reader", "Log 10 pages on 5 different days", "🌙",
                MilestoneCategory.FUN, 5, snapshot.bedtimeDaysWithTenPages),
            milestone("genre_mixer", "Genre Mixer", "Finish books by 3 different authors", "🎭",
                MilestoneCategory.FUN, 3, snapshot.distinctFinishedAuthors),
            milestone("classroom_champ", "Classroom Champ", "Read 300 pages in one month", "🏫",
                MilestoneCategory.PAGES, 300, snapshot.pagesReadThisMonth),
            milestone("reward_yourself", "Reward Yourself", "Spend Reading Rewards on a real treat!", "🎁",
                MilestoneCategory.REWARDS, 1, snapshot.rewardRedemptions),
            milestone("earn_5_dollars", "Piggy Bank Pro", "Earn \$5 in Reading Rewards", "🐷",
                MilestoneCategory.REWARDS, 500, snapshot.totalPages,
                displayOverride = String.format("\$%.2f / \$5.00", dollars.coerceAtMost(5.0))),
            milestone("earn_10_dollars", "Treasure Keeper", "Earn \$10 in Reading Rewards", "💎",
                MilestoneCategory.REWARDS, 1000, snapshot.totalPages,
                displayOverride = String.format("\$%.2f / \$10.00", dollars.coerceAtMost(10.0))),
        )
    }

    fun unlockedCount(milestones: List<Milestone>): Int = milestones.count { it.isUnlocked }

    private fun milestone(
        id: String,
        title: String,
        description: String,
        emoji: String,
        category: MilestoneCategory,
        target: Int,
        current: Int,
        displayOverride: String? = null,
    ): Milestone {
        val clamped = current.coerceAtMost(target)
        return Milestone(
            id = id,
            title = title,
            description = description,
            emoji = emoji,
            category = category,
            target = target,
            current = clamped,
            isUnlocked = current >= target,
            displayValue = displayOverride,
        )
    }
}