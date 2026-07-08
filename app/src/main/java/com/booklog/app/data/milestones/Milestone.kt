package com.booklog.app.data.milestones

enum class MilestoneCategory(val label: String) {
    REWARDS("Reading Rewards"),
    BOOKS("Book Goals"),
    PAGES("Page Goals"),
    FUN("Fun Challenges"),
}

data class Milestone(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: MilestoneCategory,
    val target: Int,
    val current: Int,
    val isUnlocked: Boolean,
    val isFeatured: Boolean = false,
    val displayValue: String? = null,
) {
    val progress: Float
        get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)

    val progressLabel: String
        get() = when {
            displayValue != null -> displayValue
            isUnlocked -> "Unlocked!"
            target <= 1 -> if (isUnlocked) "Done!" else "Almost there!"
            else -> "$current / $target"
        }
}