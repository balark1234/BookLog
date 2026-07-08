package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun observeAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY dateAdded DESC")
    fun observeBooksByStatus(status: ReadingStatus): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: Long): Flow<Book?>

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    suspend fun getAllBooks(): List<Book>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): Book?

    @Query("SELECT * FROM books WHERE isbn = :isbn LIMIT 1")
    suspend fun getBookByIsbn(isbn: String): Book?

    @Query("SELECT * FROM books WHERE isbn = :isbn AND kidProfileId IS :kidId LIMIT 1")
    suspend fun getBookByIsbnAndKid(isbn: String, kidId: Long?): Book?

    @Query("SELECT * FROM books WHERE isbn = :isbn AND kidProfileId IS NULL LIMIT 1")
    suspend fun getBookByIsbnForParent(isbn: String): Book?

    @Query("SELECT COUNT(*) FROM books")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    fun observeCountByStatus(status: ReadingStatus): Flow<Int>

    @Query("SELECT COALESCE(SUM(pageCount), 0) FROM books WHERE status = 'FINISHED'")
    fun observePagesRead(): Flow<Int>

    @Query("SELECT COALESCE(SUM(currentPage), 0) FROM books WHERE status = 'READING'")
    fun observePagesInProgress(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE rating IS NOT NULL AND rating > 0")
    fun observeRatedBooksCount(): Flow<Int>

    @Query("SELECT COALESCE(MAX(pageCount), 0) FROM books WHERE status = 'FINISHED'")
    fun observeLongestFinishedPages(): Flow<Int>

    @Query("SELECT * FROM books WHERE kidProfileId = :kidId")
    suspend fun getBooksForKid(kidId: Long): List<Book>

    @Query("SELECT * FROM books WHERE kidProfileId IS NULL")
    suspend fun getBooksWithoutKid(): List<Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)
}