package com.booklog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_transactions")
data class RewardTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kidProfileId: Long? = null,
    val amountCents: Int,
    val category: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)