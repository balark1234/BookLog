package com.booklog.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenLibraryApi {
    @GET("api/books")
    suspend fun lookupByIsbn(
        @Query("bibkeys") bibkeys: String,
        @Query("format") format: String = "json",
        @Query("jscmd") jscmd: String = "data",
    ): Map<String, OpenLibraryBookResponse>

    @GET("isbn/{isbn}.json")
    suspend fun getIsbnRecord(@Path("isbn") isbn: String): OpenLibraryIsbnRecord

    @GET("search.json")
    suspend fun searchBooks(
        @Query("title") title: String? = null,
        @Query("author") author: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("fields") fields: String = "title,author_name,isbn,cover_i,first_publish_year,number_of_pages_median",
    ): OpenLibrarySearchResponse
}

data class OpenLibrarySearchResponse(
    val docs: List<OpenLibrarySearchDoc>? = null,
)

data class OpenLibrarySearchDoc(
    val title: String? = null,
    val author_name: List<String>? = null,
    val isbn: List<String>? = null,
    val cover_i: Long? = null,
    val first_publish_year: Int? = null,
    val number_of_pages_median: Int? = null,
)

data class OpenLibraryBookResponse(
    val title: String? = null,
    val authors: List<OpenLibraryAuthor>? = null,
    val publishers: List<OpenLibraryPublisher>? = null,
    val publish_date: String? = null,
    val number_of_pages: Int? = null,
    val cover: OpenLibraryCover? = null,
    val notes: String? = null,
    val subjects: List<String>? = null,
    val identifiers: OpenLibraryIdentifiers? = null,
)

data class OpenLibraryAuthor(val name: String? = null)
data class OpenLibraryPublisher(val name: String? = null)
data class OpenLibraryCover(val small: String? = null, val medium: String? = null, val large: String? = null)
data class OpenLibraryIdentifiers(val isbn_13: List<String>? = null, val isbn_10: List<String>? = null)

data class OpenLibraryIsbnRecord(
    val title: String? = null,
    val authors: List<OpenLibraryAuthorRef>? = null,
    val number_of_pages: Int? = null,
    val publish_date: String? = null,
    val publishers: List<String>? = null,
)

data class OpenLibraryAuthorRef(val key: String? = null)