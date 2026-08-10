package com.brk.booklogger.data.cloud

enum class LeaderboardType(val label: String, val subtitle: String) {
    READERS("Readers", "Most books finished"),
    KIDS("Young readers", "Child reader champions"),
    AUTHORS("Authors", "Most-read authors"),
    PUBLISHERS("Publishers", "Most-read publishers"),
    GENRES("Genres", "Top genres"),
    MILESTONES("Milestones", "Most badges unlocked"),
}

data class CloudUserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val booksFinished: Int = 0,
    val pagesRead: Int = 0,
    val booksTotal: Int = 0,
    val milestonesUnlocked: Int = 0,
    val householdId: String? = null,
)

data class HouseholdInfo(
    val id: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val createdBy: String = "",
    val isLinked: Boolean = false,
)

data class CloudKidProfile(
    val localId: Long = 0,
    val name: String = "",
    val emoji: String = "ðŸ“š",
    val booksFinished: Int = 0,
    val pagesRead: Int = 0,
    val milestonesUnlocked: Int = 0,
)

data class LeaderboardEntry(
    val id: String = "",
    val displayName: String = "",
    val primaryValue: Int = 0,
    val secondaryValue: Int = 0,
    val rank: Int = 0,
    val emoji: String? = null,
    val subtitle: String? = null,
    val isCurrentUser: Boolean = false,
)