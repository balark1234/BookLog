package com.booklog.app.data.repository

import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.local.KidProfileDao
import kotlinx.coroutines.flow.Flow

class KidProfileRepository(private val kidProfileDao: KidProfileDao) {
    fun observeAll(): Flow<List<KidProfile>> = kidProfileDao.observeAll()

    suspend fun getAll(): List<KidProfile> = kidProfileDao.getAll()

    suspend fun getById(id: Long): KidProfile? = kidProfileDao.getById(id)

    suspend fun save(profile: KidProfile): KidProfile {
        return if (profile.id == 0L) {
            val id = kidProfileDao.insert(profile.copy(name = profile.name.trim()))
            profile.copy(id = id)
        } else {
            kidProfileDao.update(profile.copy(name = profile.name.trim()))
            profile
        }
    }

    suspend fun delete(profile: KidProfile) = kidProfileDao.delete(profile)
}