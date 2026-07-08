package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.booklog.app.data.rewards.RewardDirection
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardTransactionDao {
    @Insert
    suspend fun insert(transaction: RewardTransaction): Long

    @Query("SELECT * FROM reward_transactions WHERE kidProfileId IS NULL ORDER BY createdAt DESC")
    fun observeForParent(): Flow<List<RewardTransaction>>

    @Query("SELECT * FROM reward_transactions WHERE kidProfileId = :kidId ORDER BY createdAt DESC")
    fun observeForKid(kidId: Long): Flow<List<RewardTransaction>>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions
        WHERE kidProfileId IS NULL AND direction = :direction
        """,
    )
    suspend fun sumCentsForParent(direction: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions
        WHERE kidProfileId = :kidId AND direction = :direction
        """,
    )
    suspend fun sumCentsForKid(kidId: Long, direction: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions
        WHERE kidProfileId IS NULL AND direction = :direction
        """,
    )
    fun observeSumCentsForParent(direction: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions
        WHERE kidProfileId = :kidId AND direction = :direction
        """,
    )
    fun observeSumCentsForKid(kidId: Long, direction: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM reward_transactions WHERE kidProfileId IS NULL")
    suspend fun countForParent(): Int

    @Query("SELECT COUNT(*) FROM reward_transactions WHERE kidProfileId = :kidId")
    suspend fun countForKid(kidId: Long): Int

    // Legacy helpers for redemption totals
    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId IS NULL AND direction = 'DEBIT'")
    fun observeRedeemedCentsForParent(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId = :kidId AND direction = 'DEBIT'")
    fun observeRedeemedCentsForKid(kidId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId IS NULL AND direction = 'DEBIT'")
    suspend fun getRedeemedCentsForParent(): Int

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId = :kidId AND direction = 'DEBIT'")
    suspend fun getRedeemedCentsForKid(kidId: Long): Int
}