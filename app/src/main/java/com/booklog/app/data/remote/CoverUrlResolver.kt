package com.booklog.app.data.remote

object CoverUrlResolver {
    enum class Size(val suffix: String) {
        SMALL("S"),
        MEDIUM("M"),
        LARGE("L"),
    }

    /** Open Library Covers API — reliable ISBN-based cover URLs. */
    fun fromIsbn(isbn: String?, size: Size = Size.LARGE): String? {
        val normalized = isbn?.filter { it.isDigit() || it == 'X' || it == 'x' }?.uppercase()
        if (normalized.isNullOrBlank()) return null
        return "https://covers.openlibrary.org/b/isbn/$normalized-${size.suffix}.jpg"
    }

    fun bestAvailable(
        isbn: String?,
        apiCover: String? = null,
    ): String? = apiCover?.takeIf { it.isNotBlank() } ?: fromIsbn(isbn, Size.LARGE)
}