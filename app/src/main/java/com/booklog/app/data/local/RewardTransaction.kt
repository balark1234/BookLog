package com.booklog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.booklog.app.data.rewards.RewardDirection
import com.booklog.app.data.rewards.RewardTransactionType

@Entity(tableName = "reward_transactions")
data class RewardTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kidProfileId: Long? = null,
    val amountCents: Int,
    val category: String,
    val note: String = "",
    val direction: String = RewardDirection.DEBIT.name,
    val transactionType: String = RewardTransactionType.DEBIT_REDEEMED.name,
    val pageRewardCents: Int = 0,
    val timeRewardCents: Int = 0,
    val bonusRewardCents: Int = 0,
    val bookId: Long? = null,
    val completedBookId: Long? = null,
    val balanceBefore: Int = 0,
    val balanceAfter: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)