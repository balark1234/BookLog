package com.booklog.app.data.repository

import com.booklog.app.data.local.Book
import com.booklog.app.data.local.BookDao
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.milestones.ReadingSnapshot
import com.booklog.app.data.remote.ApiErrorMapper
import com.booklog.app.data.remote.CoverUrlResolver
import com.booklog.app.data.remote.BookSearchResult
import com.booklog.app.data.remote.OpenLibraryApi
import com.booklog.app.data.remote.OpenLibraryBookResponse
import com.booklog.app.data.remote.OpenLibrarySearchDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class BookRepository(private val bookDao: BookDao) {
    private val api: OpenLibraryApi = Retrofit.Builder()
        .baseUrl("https://openlibrary.org/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenLibraryApi::class.java)

    fun observeAllBooks(): Flow<List<Book>> = bookDao.observeAllBooks()
    fun observeBooksByStatus(status: ReadingStatus): Flow<List<Book>> =
        bookDao.observeBooksByStatus(status)
    fun observeBook(id: Long): Flow<Book?> = bookDao.observeBook(id)
    fun observeTotalCount(): Flow<Int> = bookDao.observeTotalCount()
    fun observeCountByStatus(status: ReadingStatus): Flow<Int> =
        bookDao.observeCountByStatus(status)
    fun observePagesRead(): Flow<Int> = bookDao.observePagesRead()
    fun observePagesInProgress(): Flow<Int> = bookDao.observePagesInProgress()
    fun observeRatedBooksCount(): Flow<Int> = bookDao.observeRatedBooksCount()
    fun observeLongestFinishedPages(): Flow<Int> = bookDao.observeLongestFinishedPages()

    fun observeReadingSnapshot(): Flow<ReadingSnapshot> =
        bookDao.observeAllBooks().map { books ->
            ReadingSnapshot(
                totalBooks = books.size,
                wantToRead = books.count { it.status == ReadingStatus.WANT_TO_READ },
                reading = books.count { it.status == ReadingStatus.READING },
                finished = books.count { it.status == ReadingStatus.FINISHED },
                pagesFinished = books
                    .filter { it.status == ReadingStatus.FINISHED }
                    .sumOf { it.pageCount ?: 0 },
                pagesInProgress = books
                    .filter { it.status == ReadingStatus.READING }
                    .sumOf { it.currentPage ?: 0 },
                ratedBooks = books.count { (it.rating ?: 0f) > 0f },
                longestFinishedPages = books
                    .filter { it.status == ReadingStatus.FINISHED }
                    .maxOfOrNull { it.pageCount ?: 0 } ?: 0,
            )
        }

    suspend fun getBook(id: Long): Book? = bookDao.getBook(id)
    suspend fun getAllBooks(): List<Book> = bookDao.getAllBooks()

    suspend fun getBooksForProfile(kidProfileId: Long?): List<Book> = when (kidProfileId) {
        null -> bookDao.getBooksWithoutKid()
        else -> bookDao.getBooksForKid(kidProfileId)
    }
    suspend fun saveBook(book: Book): Long {
        val withCover = book.copy(coverUrl = CoverUrlResolver.bestAvailable(book.isbn, book.coverUrl))
        return bookDao.insert(withCover)
    }
    suspend fun updateBook(book: Book) = bookDao.update(
        book.copy(coverUrl = CoverUrlResolver.bestAvailable(book.isbn, book.coverUrl))
    )
    suspend fun deleteBook(book: Book) = bookDao.delete(book)

    suspend fun lookupByIsbn(rawIsbn: String): Result<Book> {
        val isbn = normalizeIsbn(rawIsbn)
        if (isbn.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid ISBN number."))
        }

        bookDao.getBookByIsbn(isbn)?.let { return Result.success(it) }

        return try {
            val key = "ISBN:$isbn"
            val response = runCatching { api.lookupByIsbn(bibkeys = key) }.getOrNull()
            val data = response?.get(key)
            val book = when {
                data != null -> data.toBook(isbn)
                else -> fetchIsbnRecordOrThrow(isbn)
            }
            Result.success(book.withCover(isbn))
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.success(
                    Book(
                        isbn = isbn,
                        title = "",
                        author = "",
                        coverUrl = CoverUrlResolver.fromIsbn(isbn),
                    )
                )
            } else {
                Result.failure(Exception(ApiErrorMapper.friendlyMessage(e)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.friendlyMessage(e)))
        }
    }

    suspend fun searchByTitleAndAuthor(title: String, author: String): Result<List<BookSearchResult>> {
        val trimmedTitle = title.trim()
        val trimmedAuthor = author.trim()
        if (trimmedTitle.isBlank() && trimmedAuthor.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a title or author to search"))
        }
        return try {
            val response = api.searchBooks(
                title = trimmedTitle.takeIf { it.isNotBlank() },
                author = trimmedAuthor.takeIf { it.isNotBlank() },
                limit = 12,
            )
            val results = response.docs.orEmpty()
                .mapNotNull { it.toSearchResult() }
                .distinctBy { "${it.title.lowercase()}|${it.author.lowercase()}" }
            if (results.isEmpty()) {
                Result.failure(Exception("No books found. Try different spelling or add manually."))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.friendlyMessage(e)))
        }
    }

    private fun OpenLibrarySearchDoc.toSearchResult(): BookSearchResult? {
        val bookTitle = title?.trim().orEmpty()
        if (bookTitle.isBlank()) return null
        val isbn = isbn?.firstOrNull { it.length in 10..13 }
        val authorName = author_name?.joinToString(", ").orEmpty().ifBlank { "Unknown Author" }
        val cover = cover_i?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
        return BookSearchResult(
            title = bookTitle,
            author = authorName,
            isbn = isbn,
            coverUrl = CoverUrlResolver.bestAvailable(isbn, cover),
            pageCount = number_of_pages_median,
            publishedYear = first_publish_year?.toString(),
        )
    }

    private suspend fun fetchIsbnRecordOrThrow(isbn: String): Book {
        return try {
            api.getIsbnRecord(isbn).toBook(isbn)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Book(isbn = isbn, title = "", author = "")
            } else {
                throw e
            }
        }
    }

    private fun OpenLibraryBookResponse.toBook(isbn: String): Book {
        val authorNames = authors?.mapNotNull { it.name }?.joinToString(", ")
            ?: "Unknown Author"
        return Book(
            isbn = isbn,
            title = title ?: "",
            author = authorNames,
            coverUrl = CoverUrlResolver.bestAvailable(
                isbn,
                cover?.large ?: cover?.medium ?: cover?.small,
            ),
            pageCount = number_of_pages,
            publishedYear = publish_date,
            description = notes,
            publisher = publishers?.firstOrNull()?.name,
            genre = subjects?.firstOrNull(),
        )
    }

    private fun com.booklog.app.data.remote.OpenLibraryIsbnRecord.toBook(isbn: String): Book {
        val authorNames = authors?.joinToString(", ") {
            it.key?.substringAfterLast("/")?.replace("_", " ") ?: "Unknown"
        } ?: ""
        return Book(
            isbn = isbn,
            title = title ?: "",
            author = authorNames,
            pageCount = number_of_pages,
            publishedYear = publish_date,
            coverUrl = CoverUrlResolver.fromIsbn(isbn),
            publisher = publishers?.firstOrNull(),
        )
    }

    private fun Book.withCover(isbn: String) = copy(
        coverUrl = CoverUrlResolver.bestAvailable(isbn, coverUrl),
    )

    companion object {
        fun normalizeIsbn(raw: String): String =
            raw.filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()
    }
}