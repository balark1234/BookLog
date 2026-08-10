package com.brk.booklogger.data.profiles

import com.brk.booklogger.data.local.BookDao
import com.brk.booklogger.data.local.CompletedBookDao
import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.data.local.KidProfileDao
import com.brk.booklogger.data.local.ReaderProfileType
import com.brk.booklogger.data.local.ReadingDayLogDao
import com.brk.booklogger.data.local.RewardTransactionDao
import java.util.UUID

/**
 * Ensures at least one adult reader exists and migrates legacy "Parent" (null profile) rows.
 */
class ReaderBootstrap(
    private val kidProfileDao: KidProfileDao,
    private val bookDao: BookDao,
    private val readingDayLogDao: ReadingDayLogDao,
    private val rewardTransactionDao: RewardTransactionDao,
    private val completedBookDao: CompletedBookDao,
    private val activeKidPreferences: ActiveKidPreferences,
) {
    /**
     * @return active reader id after bootstrap (never null once an adult exists)
     */
    suspend fun ensureDefaultAdult(displayName: String? = null): Long {
        val adults = kidProfileDao.getByType(ReaderProfileType.ADULT.name)
        val adult = adults.firstOrNull() ?: createDefaultAdult(displayName)
        reassignLegacyParentData(adult.id)

        val active = activeKidPreferences.getActiveKidId()
        if (active == null || kidProfileDao.getById(active) == null) {
            activeKidPreferences.setActiveKidId(adult.id)
        }
        // Clear legacy "parent selected" so we always track a concrete reader
        if (activeKidPreferences.getActiveKidId() == null) {
            activeKidPreferences.setActiveKidId(adult.id)
        }
        return activeKidPreferences.getActiveKidId() ?: adult.id
    }

    private suspend fun createDefaultAdult(displayName: String?): KidProfile {
        val name = displayName?.trim().orEmpty().ifBlank { "Me" }
        val profile = KidProfile(
            name = name,
            emoji = "👤",
            profileType = ReaderProfileType.ADULT.name,
            cloudId = UUID.randomUUID().toString(),
        )
        val id = kidProfileDao.insert(profile)
        return profile.copy(id = id)
    }

    private suspend fun reassignLegacyParentData(readerId: Long) {
        bookDao.reassignParentBooksToReader(readerId)
        readingDayLogDao.reassignParentRowsToReader(readerId)
        rewardTransactionDao.reassignParentRowsToReader(readerId)
        completedBookDao.reassignParentRowsToReader(readerId)
    }
}
