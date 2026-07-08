package com.booklog.app.data.audio

import com.booklog.app.data.milestones.MilestoneEngine
import com.booklog.app.data.milestones.MilestonePreferences
import com.booklog.app.data.milestones.ReadingSnapshotComputer
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.RewardRepository

class MilestoneCelebrationCoordinator(
    private val bookRepository: BookRepository,
    private val rewardRepository: RewardRepository,
    private val milestonePreferences: MilestonePreferences,
    private val audioManager: AppAudioManager,
) {
    suspend fun checkAndCelebrate(kidProfileId: Long?) {
        val books = bookRepository.getBooksForProfile(kidProfileId)
        val logs = rewardRepository.getReadingLogs(kidProfileId)
        val redeemedCents = rewardRepository.getRedeemedCents(kidProfileId)
        val redemptionCount = rewardRepository.redemptionCount(kidProfileId)
        val snapshot = ReadingSnapshotComputer.compute(
            books = books,
            readingLogs = logs,
            booksScanned = milestonePreferences.getBooksScanned(),
            totalRedeemedCents = redeemedCents,
            rewardRedemptions = redemptionCount,
        )
        audioManager.celebrateNewMilestones(MilestoneEngine.compute(snapshot))
    }
}