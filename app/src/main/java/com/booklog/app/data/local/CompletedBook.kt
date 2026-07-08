package com.booklog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_books")
data class CompletedBook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kidProfileId: Long? = null,
    val bookId: Long,
    val isbn: String? = null,
    val title: String,
    val author: String,
    val pageCount: Int? = null,
    val minutesRead: Int,
    val dateCompleted: Long = System.currentTimeMillis(),
    val readCountNumber: Int = 1,
    val isReread: Boolean = false,
    val pageRewardCents: Int = 0,
    val timeRewardCents: Int = 0,
    val totalRewardCents: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)