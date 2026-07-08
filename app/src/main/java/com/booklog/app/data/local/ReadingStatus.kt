package com.booklog.app.data.local

enum class ReadingStatus(val label: String) {
    WANT_TO_READ("Want to Read"),
    READING("Reading"),
    FINISHED("Finished");

    companion object {
        fun fromLabel(label: String): ReadingStatus =
            entries.firstOrNull { it.label == label } ?: WANT_TO_READ
    }
}