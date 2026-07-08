package com.booklog.app.data.remote

data class BookSearchResult(
    val title: String,
    val author: String,
    val isbn: String? = null,
    val coverUrl: String? = null,
    val pageCount: Int? = null,
    val publishedYear: String? = null,
)