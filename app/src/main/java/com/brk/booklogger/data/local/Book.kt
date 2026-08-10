package com.brk.booklogger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isbn: String? = null,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val pageCount: Int? = null,
    val publishedYear: String? = null,
    val description: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    /** Local reader profile id (child or adult). */
    val kidProfileId: Long? = null,
    /** Stable id for multi-device / partner household sync. */
    val cloudId: String? = null,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val rating: Float? = null,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val dateStarted: Long? = null,
    val dateFinished: Long? = null,
    val currentPage: Int? = null,
)
