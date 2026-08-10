package com.brk.booklogger

import android.app.Application
import com.brk.booklogger.data.audio.AppAudioManager
import com.brk.booklogger.data.audio.AudioPreferences
import com.brk.booklogger.data.audio.MilestoneCelebrationCoordinator
import com.brk.booklogger.data.audio.MilestoneCelebrationPreferences
import com.brk.booklogger.data.cloud.CloudRepository
import com.brk.booklogger.data.local.Book
import com.brk.booklogger.data.local.BookDatabase
import com.brk.booklogger.data.milestones.MilestonePreferences
import com.brk.booklogger.data.profiles.ActiveKidPreferences
import com.brk.booklogger.data.profiles.GuestPreferences
import com.brk.booklogger.data.profiles.HouseholdPreferences
import com.brk.booklogger.data.profiles.ReaderBootstrap
import com.brk.booklogger.data.repository.BookRepository
import com.brk.booklogger.data.repository.KidProfileRepository
import com.brk.booklogger.data.repository.RewardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BookLogApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: BookRepository
        private set

    lateinit var kidProfileRepository: KidProfileRepository
        private set

    lateinit var cloudRepository: CloudRepository
        private set

    lateinit var milestonePreferences: MilestonePreferences
        private set

    lateinit var activeKidPreferences: ActiveKidPreferences
        private set

    lateinit var guestPreferences: GuestPreferences
        private set

    lateinit var householdPreferences: HouseholdPreferences
        private set

    lateinit var rewardRepository: RewardRepository
        private set

    lateinit var audioPreferences: AudioPreferences
        private set

    lateinit var audioManager: AppAudioManager
        private set

    lateinit var milestoneCelebrationCoordinator: MilestoneCelebrationCoordinator
        private set

    lateinit var readerBootstrap: ReaderBootstrap
        private set

    override fun onCreate() {
        super.onCreate()
        val database = BookDatabase.getInstance(this)
        repository = BookRepository(database.bookDao())
        kidProfileRepository = KidProfileRepository(database.kidProfileDao())
        rewardRepository = RewardRepository(
            database.rewardTransactionDao(),
            database.readingDayLogDao(),
            database.completedBookDao(),
            database.bookDao(),
        )
        householdPreferences = HouseholdPreferences(this)
        cloudRepository = CloudRepository(repository, kidProfileRepository, householdPreferences)
        milestonePreferences = MilestonePreferences(this)
        activeKidPreferences = ActiveKidPreferences(this)
        guestPreferences = GuestPreferences(this)
        readerBootstrap = ReaderBootstrap(
            kidProfileDao = database.kidProfileDao(),
            bookDao = database.bookDao(),
            readingDayLogDao = database.readingDayLogDao(),
            rewardTransactionDao = database.rewardTransactionDao(),
            completedBookDao = database.completedBookDao(),
            activeKidPreferences = activeKidPreferences,
        )
        audioPreferences = AudioPreferences(this)
        audioManager = AppAudioManager(
            context = this,
            audioPreferences = audioPreferences,
            celebrationPreferences = MilestoneCelebrationPreferences(this),
        )
        milestoneCelebrationCoordinator = MilestoneCelebrationCoordinator(
            bookRepository = repository,
            rewardRepository = rewardRepository,
            milestonePreferences = milestonePreferences,
            audioManager = audioManager,
        )
        appScope.launch {
            val name = cloudRepository.currentUser?.displayName
            readerBootstrap.ensureDefaultAdult(name)
        }
    }

    fun recordBookScanned() {
        milestonePreferences.recordBookScanned()
    }

    fun syncBookToCloud(book: Book) {
        if (cloudRepository.currentUser == null) return
        appScope.launch {
            cloudRepository.syncBook(book)
        }
    }

    fun celebrateMilestonesForActiveProfile() {
        appScope.launch {
            milestoneCelebrationCoordinator.checkAndCelebrate(activeKidPreferences.getActiveKidId())
        }
    }

    fun pullHouseholdIfLinked() {
        if (cloudRepository.currentUser == null) return
        appScope.launch {
            cloudRepository.pullLibraryFromCloud()
        }
    }
}
