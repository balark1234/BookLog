package com.booklog.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booklog.app.data.cloud.CloudRepository
import com.booklog.app.data.local.RewardTransaction
import com.booklog.app.data.milestones.Milestone
import com.booklog.app.data.milestones.MilestoneCategory
import com.booklog.app.data.milestones.MilestoneEngine
import com.booklog.app.data.milestones.MilestonePreferences
import com.booklog.app.data.milestones.ReadingSnapshot
import com.booklog.app.data.milestones.ReadingSnapshotComputer
import com.booklog.app.data.profiles.ActiveKidPreferences
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.RewardRepository
import com.booklog.app.data.rewards.RewardPurchaseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MilestonesUiState(
    val snapshot: ReadingSnapshot = ReadingSnapshot(),
    val milestones: List<Milestone> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val transactions: List<RewardTransaction> = emptyList(),
    val activeKidId: Long? = null,
    val isRedeeming: Boolean = false,
    val redeemError: String? = null,
    val redeemSuccess: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MilestonesViewModel(
    private val repository: BookRepository,
    private val rewardRepository: RewardRepository,
    private val activeKidPreferences: ActiveKidPreferences,
    private val milestonePreferences: MilestonePreferences,
    private val cloudRepository: CloudRepository? = null,
) : ViewModel() {
    private val scanCount = MutableStateFlow(milestonePreferences.getBooksScanned())
    private val activeKidId = MutableStateFlow(activeKidPreferences.getActiveKidId())
    private val redeemStatus = MutableStateFlow(RedeemStatus())

    val uiState: StateFlow<MilestonesUiState> = combine(
        repository.observeAllBooks(),
        scanCount,
        activeKidId,
        redeemStatus,
    ) { books, scans, kidId, redeem ->
        RewardInputs(books, scans, kidId, redeem)
    }.flatMapLatest { inputs ->
        combine(
            rewardRepository.observeTransactions(inputs.kidId),
            rewardRepository.observeRedeemedCents(inputs.kidId),
            rewardRepository.observeReadingLogs(inputs.kidId),
        ) { transactions, redeemedCents, logs ->
            val profileBooks = inputs.books.filter { book ->
                when (inputs.kidId) {
                    null -> book.kidProfileId == null
                    else -> book.kidProfileId == inputs.kidId
                }
            }
            val snapshot = ReadingSnapshotComputer.compute(
                books = profileBooks,
                readingLogs = logs,
                booksScanned = inputs.scans,
                totalRedeemedCents = redeemedCents,
                rewardRedemptions = transactions.size,
            )
            val milestones = MilestoneEngine.compute(snapshot)
            MilestonesUiState(
                snapshot = snapshot,
                milestones = milestones,
                unlockedCount = MilestoneEngine.unlockedCount(milestones),
                totalCount = milestones.size,
                transactions = transactions,
                activeKidId = inputs.kidId,
                isRedeeming = inputs.redeem.isRedeeming,
                redeemError = inputs.redeem.error,
                redeemSuccess = inputs.redeem.success,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MilestonesUiState())

    fun refreshScanCount() {
        scanCount.update { milestonePreferences.getBooksScanned() }
    }

    fun refreshActiveKid() {
        activeKidId.update { activeKidPreferences.getActiveKidId() }
    }

    fun clearRedeemMessages() {
        redeemStatus.update { it.copy(error = null, success = null) }
    }

    fun redeemReward(
        amountDollars: String,
        category: RewardPurchaseCategory,
        note: String,
        availableBalanceCents: Int,
    ) {
        val parsed = amountDollars.trim().removePrefix("$").toDoubleOrNull()
        if (parsed == null || parsed <= 0) {
            redeemStatus.update { it.copy(error = "Enter a valid dollar amount") }
            return
        }
        val amountCents = (parsed * 100).toInt()
        viewModelScope.launch {
            redeemStatus.update { it.copy(isRedeeming = true, error = null, success = null) }
            rewardRepository.redeem(
                kidProfileId = activeKidId.value,
                amountCents = amountCents,
                category = category,
                note = note,
                availableBalanceCents = availableBalanceCents,
            ).onSuccess { transaction ->
                redeemStatus.update {
                    it.copy(
                        isRedeeming = false,
                        success = "Redeemed ${RewardRepository.formatCents(transaction.amountCents)} for ${category.label}!",
                    )
                }
            }.onFailure { e ->
                redeemStatus.update {
                    it.copy(isRedeeming = false, error = e.message ?: "Couldn't redeem reward")
                }
            }
        }
    }

    fun syncMilestonesToCloud(snapshot: ReadingSnapshot, milestones: List<Milestone>) {
        val cloud = cloudRepository ?: return
        if (cloud.currentUser == null) return
        viewModelScope.launch {
            cloud.syncMilestones(snapshot, milestones)
        }
    }

    fun milestonesByCategory(category: MilestoneCategory, state: MilestonesUiState): List<Milestone> =
        state.milestones.filter { it.category == category }

    private data class RewardInputs(
        val books: List<com.booklog.app.data.local.Book>,
        val scans: Int,
        val kidId: Long?,
        val redeem: RedeemStatus,
    )

    private data class RedeemStatus(
        val isRedeeming: Boolean = false,
        val error: String? = null,
        val success: String? = null,
    )

    class Factory(
        private val repository: BookRepository,
        private val rewardRepository: RewardRepository,
        private val activeKidPreferences: ActiveKidPreferences,
        private val milestonePreferences: MilestonePreferences,
        private val cloudRepository: CloudRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MilestonesViewModel(
                repository,
                rewardRepository,
                activeKidPreferences,
                milestonePreferences,
                cloudRepository,
            ) as T
    }
}