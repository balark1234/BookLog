package com.booklog.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KidProfileDao {
    @Query("SELECT * FROM kid_profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<KidProfile>>

    @Query("SELECT * FROM kid_profiles ORDER BY createdAt ASC")
    suspend fun getAll(): List<KidProfile>

    @Query("SELECT * FROM kid_profiles WHERE id = :id")
    suspend fun getById(id: Long): KidProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: KidProfile): Long

    @Update
    suspend fun update(profile: KidProfile)

    @Delete
    suspend fun delete(profile: KidProfile)

    @Query("SELECT COUNT(*) FROM kid_profiles")
    suspend fun count(): Int
}