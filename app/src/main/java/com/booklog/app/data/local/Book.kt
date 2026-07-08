package com.booklog.app.data.local

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
    val kidProfileId: Long? = null,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val rating: Float? = null,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val dateStarted: Long? = null,
    val dateFinished: Long? = null,
    val currentPage: Int? = null,
)