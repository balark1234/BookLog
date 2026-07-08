package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedBookDao {
    @Query("SELECT * FROM completed_books WHERE kidProfileId IS NULL ORDER BY dateCompleted DESC")
    fun observeForParent(): Flow<List<CompletedBook>>

    @Query("SELECT * FROM completed_books WHERE kidProfileId = :kidId ORDER BY dateCompleted DESC")
    fun observeForKid(kidId: Long): Flow<List<CompletedBook>>

    @Query("SELECT * FROM completed_books ORDER BY dateCompleted DESC")
    fun observeAll(): Flow<List<CompletedBook>>

    @Query(
        """
        SELECT * FROM completed_books
        WHERE isbn = :isbn AND kidProfileId IS :kidId
        ORDER BY dateCompleted DESC
        """,
    )
    suspend fun getHistoryByIsbnAndKid(isbn: String, kidId: Long?): List<CompletedBook>

    @Query(
        """
        SELECT COUNT(*) FROM completed_books
        WHERE isbn = :isbn AND kidProfileId IS :kidId
        """,
    )
    suspend fun countByIsbnAndKid(isbn: String, kidId: Long?): Int

    @Query("SELECT COALESCE(SUM(minutesRead), 0) FROM completed_books WHERE kidProfileId IS NULL")
    suspend fun totalMinutesForParent(): Int

    @Query("SELECT COALESCE(SUM(minutesRead), 0) FROM completed_books WHERE kidProfileId = :kidId")
    suspend fun totalMinutesForKid(kidId: Long): Int

    @Query("SELECT COUNT(*) FROM completed_books WHERE kidProfileId IS NULL")
    suspend fun countForParent(): Int

    @Query("SELECT COUNT(*) FROM completed_books WHERE kidProfileId = :kidId")
    suspend fun countForKid(kidId: Long): Int

    @Query("SELECT COALESCE(SUM(pageCount), 0) FROM completed_books WHERE kidProfileId IS NULL")
    suspend fun totalPagesForParent(): Int

    @Query("SELECT COALESCE(SUM(pageCount), 0) FROM completed_books WHERE kidProfileId = :kidId")
    suspend fun totalPagesForKid(kidId: Long): Int

    @Query("SELECT * FROM completed_books WHERE kidProfileId IS NULL ORDER BY dateCompleted DESC")
    suspend fun getAllForParent(): List<CompletedBook>

    @Query("SELECT * FROM completed_books WHERE kidProfileId = :kidId ORDER BY dateCompleted DESC")
    suspend fun getAllForKid(kidId: Long): List<CompletedBook>

    @Query("SELECT * FROM completed_books WHERE id = :id")
    suspend fun getById(id: Long): CompletedBook?

    @Insert
    suspend fun insert(completed: CompletedBook): Long

    @Delete
    suspend fun delete(completed: CompletedBook)
}