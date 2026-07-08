package com.booklog.app.data.repository

import com.booklog.app.data.local.ReadingDayLog
import com.booklog.app.data.local.ReadingDayLogDao
import com.booklog.app.data.local.RewardTransaction
import com.booklog.app.data.local.RewardTransactionDao
import com.booklog.app.data.rewards.RewardPurchaseCategory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RewardRepository(
    private val rewardTransactionDao: RewardTransactionDao,
    private val readingDayLogDao: ReadingDayLogDao,
) {
    fun observeTransactions(kidProfileId: Long?): Flow<List<RewardTransaction>> =
        if (kidProfileId == null) rewardTransactionDao.observeForParent()
        else rewardTransactionDao.observeForKid(kidProfileId)

    fun observeRedeemedCents(kidProfileId: Long?): Flow<Int> =
        if (kidProfileId == null) rewardTransactionDao.observeRedeemedCentsForParent()
        else rewardTransactionDao.observeRedeemedCentsForKid(kidProfileId)

    fun observeReadingLogs(kidProfileId: Long?): Flow<List<ReadingDayLog>> =
        if (kidProfileId == null) readingDayLogDao.observeForParent()
        else readingDayLogDao.observeForKid(kidProfileId)

    suspend fun redeem(
        kidProfileId: Long?,
        amountCents: Int,
        category: RewardPurchaseCategory,
        note: String,
        availableBalanceCents: Int,
    ): Result<RewardTransaction> {
        if (amountCents <= 0) {
            return Result.failure(IllegalArgumentException("Enter an amount greater than \$0"))
        }
        if (amountCents > availableBalanceCents) {
            return Result.failure(
                IllegalArgumentException(
                    "Not enough rewards! Balance is ${formatCents(availableBalanceCents)}",
                ),
            )
        }
        val transaction = RewardTransaction(
            kidProfileId = kidProfileId,
            amountCents = amountCents,
            category = category.name,
            note = note.trim(),
        )
        val id = rewardTransactionDao.insert(transaction)
        return Result.success(transaction.copy(id = id))
    }

    suspend fun recordPagesRead(
        kidProfileId: Long?,
        bookId: Long,
        pagesDelta: Int,
    ) {
        if (pagesDelta <= 0) return
        val dayKey = todayKey()
        readingDayLogDao.insert(
            ReadingDayLog(
                kidProfileId = kidProfileId,
                bookId = bookId,
                dayKey = dayKey,
                pagesLogged = pagesDelta,
            ),
        )
    }

    suspend fun getReadingLogs(kidProfileId: Long?): List<ReadingDayLog> =
        if (kidProfileId == null) readingDayLogDao.getAllForParent()
        else readingDayLogDao.getAllForKid(kidProfileId)

    suspend fun redemptionCount(kidProfileId: Long?): Int =
        if (kidProfileId == null) rewardTransactionDao.countForParent()
        else rewardTransactionDao.countForKid(kidProfileId)

    suspend fun getRedeemedCents(kidProfileId: Long?): Int =
        if (kidProfileId == null) rewardTransactionDao.getRedeemedCentsForParent()
        else rewardTransactionDao.getRedeemedCentsForKid(kidProfileId)

    companion object {
        fun todayKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun formatCents(cents: Int): String = String.format(Locale.US, "$%.2f", cents / 100.0)

        fun categoryLabel(categoryName: String): String =
            RewardPurchaseCategory.entries
                .find { it.name == categoryName }
                ?.let { "${it.emoji} ${it.label}" }
                ?: categoryName
    }
}