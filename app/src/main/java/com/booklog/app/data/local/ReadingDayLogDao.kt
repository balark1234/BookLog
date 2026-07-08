package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDayLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReadingDayLog): Long

    @Query("SELECT * FROM reading_day_logs WHERE kidProfileId IS NULL ORDER BY loggedAt DESC")
    fun observeForParent(): Flow<List<ReadingDayLog>>

    @Query("SELECT * FROM reading_day_logs WHERE kidProfileId = :kidId ORDER BY loggedAt DESC")
    fun observeForKid(kidId: Long): Flow<List<ReadingDayLog>>

    @Query("SELECT * FROM reading_day_logs WHERE kidProfileId IS NULL")
    suspend fun getAllForParent(): List<ReadingDayLog>

    @Query("SELECT * FROM reading_day_logs WHERE kidProfileId = :kidId")
    suspend fun getAllForKid(kidId: Long): List<ReadingDayLog>
}