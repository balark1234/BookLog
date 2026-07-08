package com.booklog.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_day_logs")
data class ReadingDayLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kidProfileId: Long? = null,
    val bookId: Long? = null,
    /** Local date key yyyy-MM-dd */
    val dayKey: String,
    val pagesLogged: Int,
    val loggedAt: Long = System.currentTimeMillis(),
)