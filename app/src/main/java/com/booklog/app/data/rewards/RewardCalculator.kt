package com.booklog.app.data.rewards

data class RewardBreakdown(
    val pageRewardCents: Int,
    val timeRewardCents: Int,
    val bonusRewardCents: Int,
    val totalCents: Int,
)

object RewardCalculator {
    /** $0.01 per page */
    const val CENTS_PER_PAGE = 1

    /** $0.02 per minute — time weighted 2× vs pages */
    const val CENTS_PER_MINUTE = 2

    fun calculate(pages: Int, minutes: Int, isReread: Boolean): RewardBreakdown {
        val safePages = pages.coerceAtLeast(0)
        val safeMinutes = minutes.coerceAtLeast(0)
        val pageReward = if (isReread) 0 else safePages * CENTS_PER_PAGE
        val timeReward = safeMinutes * CENTS_PER_MINUTE
        return RewardBreakdown(
            pageRewardCents = pageReward,
            timeRewardCents = timeReward,
            bonusRewardCents = 0,
            totalCents = pageReward + timeReward,
        )
    }
}