package com.booklog.app.data.local

import androidx.room.TypeConverter

class ReadingStatusConverter {
    @TypeConverter
    fun fromStatus(status: ReadingStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ReadingStatus =
        runCatching { ReadingStatus.valueOf(value) }.getOrDefault(ReadingStatus.WANT_TO_READ)
}