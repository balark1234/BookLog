package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardTransactionDao {
    @Insert
    suspend fun insert(transaction: RewardTransaction): Long

    @Query("SELECT * FROM reward_transactions WHERE kidProfileId IS NULL ORDER BY createdAt DESC")
    fun observeForParent(): Flow<List<RewardTransaction>>

    @Query("SELECT * FROM reward_transactions WHERE kidProfileId = :kidId ORDER BY createdAt DESC")
    fun observeForKid(kidId: Long): Flow<List<RewardTransaction>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId IS NULL")
    fun observeRedeemedCentsForParent(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId = :kidId")
    fun observeRedeemedCentsForKid(kidId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM reward_transactions WHERE kidProfileId IS NULL")
    suspend fun countForParent(): Int

    @Query("SELECT COUNT(*) FROM reward_transactions WHERE kidProfileId = :kidId")
    suspend fun countForKid(kidId: Long): Int

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId IS NULL")
    suspend fun getRedeemedCentsForParent(): Int

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM reward_transactions WHERE kidProfileId = :kidId")
    suspend fun getRedeemedCentsForKid(kidId: Long): Int
}