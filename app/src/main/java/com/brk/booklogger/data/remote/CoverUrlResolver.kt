package com.brk.booklogger.data.remote

/**
 * Builds cover image URLs from catalog sources (Open Library).
 *
 * Firebase may store the cover **URL string** (text). Image **bytes** are never
 * uploaded; Coil downloads and caches them on each device.
 * If cloud has no coverUrl, resolve via ISBN / title search after pull.
 */
object CoverUrlResolver {
    enum class Size(val suffix: String) {
        SMALL("S"),
        MEDIUM("M"),
        LARGE("L"),
    }

    /** Open Library Covers API — ISBN-based cover URL (image fetched on demand by Coil). */
    fun fromIsbn(isbn: String?, size: Size = Size.LARGE): String? {
        val normalized = isbn?.filter { it.isDigit() || it == 'X' || it == 'x' }?.uppercase()
        if (normalized.isNullOrBlank()) return null
        return "https://covers.openlibrary.org/b/isbn/$normalized-${size.suffix}.jpg"
    }

    /**
     * Prefer a known API cover, else derive from ISBN.
     * Does not store image bytes — only returns a remote URL for local use/cache.
     */
    fun bestAvailable(
        isbn: String?,
        apiCover: String? = null,
    ): String? = apiCover?.takeIf { it.isNotBlank() } ?: fromIsbn(isbn, Size.LARGE)

    /**
     * Resolve cover for cloud-imported books (never trust cloud coverUrl).
     * ISBN path is preferred; [apiCoverFromSearch] can be filled after a title search.
     */
    fun forCloudImport(
        isbn: String?,
        apiCoverFromSearch: String? = null,
    ): String? = bestAvailable(isbn = isbn, apiCover = apiCoverFromSearch)
}
