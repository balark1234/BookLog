package com.brk.booklogger.data.repository

import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.data.local.KidProfileDao
import com.brk.booklogger.data.local.ReaderProfileType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class KidProfileRepository(private val kidProfileDao: KidProfileDao) {
    fun observeAll(): Flow<List<KidProfile>> = kidProfileDao.observeAll()

    suspend fun getAll(): List<KidProfile> = kidProfileDao.getAll()

    suspend fun getById(id: Long): KidProfile? = kidProfileDao.getById(id)

    suspend fun getByCloudId(cloudId: String): KidProfile? = kidProfileDao.getByCloudId(cloudId)

    suspend fun getAdults(): List<KidProfile> =
        kidProfileDao.getByType(ReaderProfileType.ADULT.name)

    suspend fun save(profile: KidProfile): KidProfile {
        val withCloudId = if (profile.cloudId.isNullOrBlank()) {
            profile.copy(cloudId = UUID.randomUUID().toString())
        } else {
            profile
        }
        return if (withCloudId.id == 0L) {
            val id = kidProfileDao.insert(withCloudId.copy(name = withCloudId.name.trim()))
            withCloudId.copy(id = id, name = withCloudId.name.trim())
        } else {
            val cleaned = withCloudId.copy(name = withCloudId.name.trim())
            kidProfileDao.update(cleaned)
            cleaned
        }
    }

    suspend fun delete(profile: KidProfile) = kidProfileDao.delete(profile)
}