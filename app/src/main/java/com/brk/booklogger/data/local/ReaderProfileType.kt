package com.brk.booklogger.data.local

enum class ReaderProfileType(val label: String) {
    CHILD("Child"),
    ADULT("Adult"),
    ;

    companion object {
        fun fromStorage(value: String?): ReaderProfileType =
            entries.find { it.name == value } ?: CHILD
    }
}
