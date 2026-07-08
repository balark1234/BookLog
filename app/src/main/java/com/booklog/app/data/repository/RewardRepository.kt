package com.booklog.app.data.repository

import com.booklog.app.data.local.Book
import com.booklog.app.data.local.BookDao
import com.booklog.app.data.local.CompletedBook
import com.booklog.app.data.local.CompletedBookDao
import com.booklog.app.data.remote.CoverUrlResolver
import com.booklog.app.data.local.ReadingDayLog
import com.booklog.app.data.local.ReadingDayLogDao
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.local.RewardTransaction
import com.booklog.app.data.local.RewardTransactionDao
import com.booklog.app.data.rewards.RewardCalculator
import com.booklog.app.data.rewards.RewardDirection
import com.booklog.app.data.rewards.RewardTransactionType
import com.booklog.app.data.rewards.RewardPurchaseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RewardRepository(
    private val rewardTransactionDao: RewardTransactionDao,
    private val readingDayLogDao: ReadingDayLogDao,
    private val completedBookDao: CompletedBookDao,
    private val bookDao: BookDao,
) {
    fun observeTransactions(kidProfileId: Long?): Flow<List<RewardTransaction>> =
        if (kidProfileId == null) rewardTransactionDao.observeForParent()
        else rewardTransactionDao.observeForKid(kidProfileId)

    fun observeCompletions(kidProfileId: Long?): Flow<List<CompletedBook>> =
        if (kidProfileId == null) completedBookDao.observeForParent()
        else completedBookDao.observeForKid(kidProfileId)

    fun observeRedeemedCents(kidProfileId: Long?): Flow<Int> =
        if (kidProfileId == null) rewardTransactionDao.observeRedeemedCentsForParent()
        else rewardTransactionDao.observeRedeemedCentsForKid(kidProfileId)

    fun observeReadingLogs(kidProfileId: Long?): Flow<List<ReadingDayLog>> =
        if (kidProfileId == null) readingDayLogDao.observeForParent()
        else readingDayLogDao.observeForKid(kidProfileId)

    fun observeEarnedCents(kidProfileId: Long?): Flow<Int> =
        if (kidProfileId == null) {
            rewardTransactionDao.observeSumCentsForParent(RewardDirection.CREDIT.name)
        } else {
            rewardTransactionDao.observeSumCentsForKid(kidProfileId, RewardDirection.CREDIT.name)
        }

    fun observeBalanceCents(kidProfileId: Long?): Flow<Int> =
        combine(
            observeEarnedCents(kidProfileId),
            observeRedeemedCents(kidProfileId),
        ) { earned, redeemed -> (earned - redeemed).coerceAtLeast(0) }

    suspend fun getBalanceCents(kidProfileId: Long?): Int {
        val credits = if (kidProfileId == null) {
            rewardTransactionDao.sumCentsForParent(RewardDirection.CREDIT.name)
        } else {
            rewardTransactionDao.sumCentsForKid(kidProfileId, RewardDirection.CREDIT.name)
        }
        val debits = if (kidProfileId == null) {
            rewardTransactionDao.sumCentsForParent(RewardDirection.DEBIT.name)
        } else {
            rewardTransactionDao.sumCentsForKid(kidProfileId, RewardDirection.DEBIT.name)
        }
        return (credits - debits).coerceAtLeast(0)
    }

    suspend fun getCompletionHistory(isbn: String?, kidProfileId: Long?): List<CompletedBook> {
        if (isbn.isNullOrBlank()) return emptyList()
        return completedBookDao.getHistoryByIsbnAndKid(isbn, kidProfileId)
    }

    suspend fun logBookCompletion(
        kidProfileId: Long?,
        metadata: Book,
        minutesRead: Int,
    ): Result<CompletedBook> {
        if (minutesRead <= 0) {
            return Result.failure(IllegalArgumentException("Enter how long they spent reading."))
        }

        val isbn = metadata.isbn
        val priorCount = if (!isbn.isNullOrBlank()) {
            completedBookDao.countByIsbnAndKid(isbn, kidProfileId)
        } else {
            0
        }
        val isReread = priorCount > 0
        val readCountNumber = priorCount + 1
        val pages = metadata.pageCount ?: 0
        val breakdown = RewardCalculator.calculate(pages, minutesRead, isReread)
        val balanceBefore = getBalanceCents(kidProfileId)

        val existing = metadata.isbn?.let { isbn ->
            if (kidProfileId == null) bookDao.getBookByIsbnForParent(isbn)
            else bookDao.getBookByIsbnAndKid(isbn, kidProfileId)
        }
        val now = System.currentTimeMillis()
        val bookId = if (existing != null) {
            val finished = existing.copy(
                status = ReadingStatus.FINISHED,
                dateStarted = existing.dateStarted ?: now,
                dateFinished = now,
                currentPage = existing.pageCount ?: existing.currentPage,
                kidProfileId = kidProfileId,
            )
            bookDao.update(
                finished.copy(
                    coverUrl = CoverUrlResolver.bestAvailable(finished.isbn, finished.coverUrl),
                ),
            )
            existing.id
        } else {
            val toSave = metadata.copy(
                kidProfileId = kidProfileId,
                status = ReadingStatus.FINISHED,
                dateAdded = now,
                dateStarted = now,
                dateFinished = now,
                currentPage = metadata.pageCount,
                coverUrl = CoverUrlResolver.bestAvailable(metadata.isbn, metadata.coverUrl),
            )
            bookDao.insert(toSave)
        }

        val completed = CompletedBook(
            kidProfileId = kidProfileId,
            bookId = bookId,
            isbn = isbn,
            title = metadata.title,
            author = metadata.author,
            pageCount = metadata.pageCount,
            minutesRead = minutesRead,
            dateCompleted = now,
            readCountNumber = readCountNumber,
            isReread = isReread,
            pageRewardCents = breakdown.pageRewardCents,
            timeRewardCents = breakdown.timeRewardCents,
            totalRewardCents = breakdown.totalCents,
        )
        val completedId = completedBookDao.insert(completed)
        val balanceAfter = balanceBefore + breakdown.totalCents

        rewardTransactionDao.insert(
            RewardTransaction(
                kidProfileId = kidProfileId,
                amountCents = breakdown.totalCents,
                category = RewardTransactionType.CREDIT_BOOK_COMPLETED.name,
                note = buildString {
                    append("Completed ${metadata.title}")
                    if (isReread) append(" (re-read #$readCountNumber)")
                },
                direction = RewardDirection.CREDIT.name,
                transactionType = RewardTransactionType.CREDIT_BOOK_COMPLETED.name,
                pageRewardCents = breakdown.pageRewardCents,
                timeRewardCents = breakdown.timeRewardCents,
                bonusRewardCents = breakdown.bonusRewardCents,
                bookId = bookId,
                completedBookId = completedId,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
            ),
        )

        if (pages > 0) {
            recordPagesRead(kidProfileId, bookId, pages)
        }

        return Result.success(completed.copy(id = completedId))
    }

    suspend fun redeem(
        kidProfileId: Long?,
        amountCents: Int,
        category: RewardPurchaseCategory,
        note: String,
    ): Result<RewardTransaction> {
        val available = getBalanceCents(kidProfileId)
        if (amountCents <= 0) {
            return Result.failure(IllegalArgumentException("Enter an amount greater than \$0"))
        }
        if (amountCents > available) {
            return Result.failure(
                IllegalArgumentException(
                    "Not enough rewards! Balance is ${formatCents(available)}",
                ),
            )
        }
        val balanceBefore = available
        val balanceAfter = available - amountCents
        val transaction = RewardTransaction(
            kidProfileId = kidProfileId,
            amountCents = amountCents,
            category = category.name,
            note = note.trim(),
            direction = RewardDirection.DEBIT.name,
            transactionType = RewardTransactionType.DEBIT_REDEEMED.name,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter,
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
        readingDayLogDao.insert(
            ReadingDayLog(
                kidProfileId = kidProfileId,
                bookId = bookId,
                dayKey = todayKey(),
                pagesLogged = pagesDelta,
            ),
        )
    }

    suspend fun getReadingLogs(kidProfileId: Long?): List<ReadingDayLog> =
        if (kidProfileId == null) readingDayLogDao.getAllForParent()
        else readingDayLogDao.getAllForKid(kidProfileId)

    suspend fun getCompletions(kidProfileId: Long?): List<CompletedBook> =
        if (kidProfileId == null) completedBookDao.getAllForParent()
        else completedBookDao.getAllForKid(kidProfileId)

    suspend fun getEarnedCents(kidProfileId: Long?): Int =
        if (kidProfileId == null) {
            rewardTransactionDao.sumCentsForParent(RewardDirection.CREDIT.name)
        } else {
            rewardTransactionDao.sumCentsForKid(kidProfileId, RewardDirection.CREDIT.name)
        }

    suspend fun redemptionCount(kidProfileId: Long?): Int =
        if (kidProfileId == null) rewardTransactionDao.countForParent()
        else rewardTransactionDao.countForKid(kidProfileId)

    suspend fun getRedeemedCents(kidProfileId: Long?): Int =
        if (kidProfileId == null) rewardTransactionDao.getRedeemedCentsForParent()
        else rewardTransactionDao.getRedeemedCentsForKid(kidProfileId)

    suspend fun totalMinutesRead(kidProfileId: Long?): Int =
        if (kidProfileId == null) completedBookDao.totalMinutesForParent()
        else completedBookDao.totalMinutesForKid(kidProfileId)

    suspend fun booksCompletedCount(kidProfileId: Long?): Int =
        if (kidProfileId == null) completedBookDao.countForParent()
        else completedBookDao.countForKid(kidProfileId)

    companion object {
        fun todayKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun formatCents(cents: Int): String = String.format(Locale.US, "$%.2f", cents / 100.0)

        fun categoryLabel(categoryName: String): String =
            RewardPurchaseCategory.entries
                .find { it.name == categoryName }
                ?.let { "${it.emoji} ${it.label}" }
                ?: categoryName

        fun transactionTypeLabel(typeName: String): String = when (typeName) {
            RewardTransactionType.CREDIT_BOOK_COMPLETED.name -> "Book completed"
            RewardTransactionType.DEBIT_REDEEMED.name -> "Reward redeemed"
            RewardTransactionType.DEBIT_PAID.name -> "Paid by parent"
            RewardTransactionType.DEBIT_REVERSAL.name -> "Reversal"
            RewardTransactionType.CREDIT_MANUAL.name -> "Manual bonus"
            else -> typeName
        }
    }
}