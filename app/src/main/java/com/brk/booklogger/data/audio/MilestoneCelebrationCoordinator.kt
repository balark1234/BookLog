package com.brk.booklogger.data.audio

import com.brk.booklogger.data.milestones.MilestoneEngine
import com.brk.booklogger.data.milestones.MilestonePreferences
import com.brk.booklogger.data.milestones.ReadingSnapshotComputer
import com.brk.booklogger.data.repository.BookRepository
import com.brk.booklogger.data.repository.RewardRepository

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
        val completions = rewardRepository.getCompletions(kidProfileId)
        val earnedCents = rewardRepository.getEarnedCents(kidProfileId)
        val balanceCents = rewardRepository.getBalanceCents(kidProfileId)
        val snapshot = ReadingSnapshotComputer.compute(
            books = books,
            readingLogs = logs,
            booksScanned = milestonePreferences.getBooksScanned(),
            totalRedeemedCents = redeemedCents,
            rewardRedemptions = redemptionCount,
            completions = completions,
            earnedCents = earnedCents,
            availableBalanceCents = balanceCents,
        )
        audioManager.celebrateNewMilestones(MilestoneEngine.compute(snapshot))
    }
}