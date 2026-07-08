package com.booklog.app.data.cloud

import com.booklog.app.data.local.Book
import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.data.milestones.Milestone
import com.booklog.app.data.milestones.MilestoneEngine
import com.booklog.app.data.milestones.ReadingSnapshot
import com.booklog.app.data.repository.BookRepository
import com.booklog.app.data.repository.KidProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CloudRepository(
    private val bookRepository: BookRepository,
    private val kidProfileRepository: KidProfileRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Account created but user is missing")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName.trim()).build()).await()
        ensureUserDocument(user, displayName.trim())
    }.map { }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        syncLocalBooksToCloud()
    }.map { }

    fun signOut() = auth.signOut()

    suspend fun syncLocalBooksToCloud(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        syncAllProfiles(user)
    }

    suspend fun syncBook(book: Book): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val userRef = firestore.collection("users").document(user.uid)
        userRef.collection("books").document(book.id.toString())
            .set(book.toCloudMap(), SetOptions.merge())
            .await()
        syncAllProfiles(user)
        if (book.status == ReadingStatus.FINISHED) {
            updateGlobalReadingStats(book)
        }
    }

    suspend fun syncKidProfiles(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        syncAllProfiles(user)
    }

    suspend fun addKidProfile(profile: KidProfile): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val userRef = firestore.collection("users").document(user.uid)
        userRef.collection("kids").document(profile.id.toString()).set(
            mapOf(
                "localId" to profile.id,
                "name" to profile.name,
                "emoji" to profile.emoji,
                "gender" to profile.gender,
                "dateOfBirth" to profile.dateOfBirth,
                "favoriteGenre" to profile.favoriteGenre,
                "notes" to profile.notes,
                "createdAt" to profile.createdAt,
            ),
            SetOptions.merge(),
        ).await()
        syncKidLeaderboard(user.uid, profile, emptyList())
    }

    suspend fun removeKidProfile(profile: KidProfile): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        firestore.collection("users").document(user.uid)
            .collection("kids").document(profile.id.toString())
            .delete()
            .await()
        firestore.collection("leaderboard_kids").document(kidLeaderboardId(user.uid, profile.id))
            .delete()
            .await()
    }

    suspend fun syncMilestones(snapshot: ReadingSnapshot, milestones: List<Milestone>): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: return@runCatching
            val unlocked = milestones.filter { it.isUnlocked }.map { it.id }
            val unlockedCount = unlocked.size
            firestore.collection("users").document(user.uid).set(
                mapOf(
                    "milestonesUnlocked" to unlockedCount,
                    "unlockedMilestoneIds" to unlocked,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
            firestore.collection("leaderboard_milestones").document(user.uid).set(
                mapOf(
                    "displayName" to (user.displayName ?: "Reader"),
                    "milestonesUnlocked" to unlockedCount,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
        }

    suspend fun fetchLeaderboard(
        type: LeaderboardType = LeaderboardType.READERS,
        limit: Long = 25,
    ): Result<List<LeaderboardEntry>> = runCatching {
        when (type) {
            LeaderboardType.READERS -> fetchReadersLeaderboard(limit)
            LeaderboardType.KIDS -> fetchKidsLeaderboard(limit)
            LeaderboardType.AUTHORS -> fetchAggregateLeaderboard("leaderboard_authors", "booksFinished", limit)
            LeaderboardType.PUBLISHERS -> fetchAggregateLeaderboard("leaderboard_publishers", "booksFinished", limit)
            LeaderboardType.GENRES -> fetchAggregateLeaderboard("leaderboard_genres", "booksFinished", limit)
            LeaderboardType.MILESTONES -> fetchMilestonesLeaderboard(limit)
        }
    }

    suspend fun fetchProfile(): Result<CloudUserProfile> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val doc = firestore.collection("users").document(user.uid).get().await()
        CloudUserProfile(
            uid = user.uid,
            displayName = doc.getString("displayName") ?: user.displayName ?: "Reader",
            email = user.email ?: "",
            booksFinished = doc.getLong("booksFinished")?.toInt() ?: 0,
            pagesRead = doc.getLong("pagesRead")?.toInt() ?: 0,
            booksTotal = doc.getLong("booksTotal")?.toInt() ?: 0,
            milestonesUnlocked = doc.getLong("milestonesUnlocked")?.toInt() ?: 0,
        )
    }

    suspend fun fetchKidProfiles(): Result<List<CloudKidProfile>> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val localKids = kidProfileRepository.getAll()
        val cloudDocs = firestore.collection("users").document(user.uid)
            .collection("kids").get().await()
        localKids.map { kid ->
            val doc = cloudDocs.documents.find { it.getLong("localId") == kid.id }
            val books = bookRepository.getBooksForProfile(kid.id)
            val stats = computeStats(books)
            CloudKidProfile(
                localId = kid.id,
                name = kid.name,
                emoji = kid.emoji,
                booksFinished = doc?.getLong("booksFinished")?.toInt() ?: stats.finished,
                pagesRead = doc?.getLong("pagesRead")?.toInt() ?: stats.pages,
                milestonesUnlocked = doc?.getLong("milestonesUnlocked")?.toInt() ?: 0,
            )
        }
    }

    private suspend fun syncAllProfiles(user: FirebaseUser) {
        val userRef = firestore.collection("users").document(user.uid)
        val allBooks = bookRepository.getAllBooks()
        val batch = firestore.batch()
        allBooks.forEach { book ->
            val doc = userRef.collection("books").document(book.id.toString())
            batch.set(doc, book.toCloudMap(), SetOptions.merge())
        }
        val parentBooks = bookRepository.getBooksForProfile(null)
        val parentStats = computeStats(parentBooks)
        val parentMilestones = computeMilestones(parentBooks)
        batch.set(
            userRef,
            mapOf(
                "displayName" to (user.displayName ?: "Reader"),
                "email" to (user.email ?: ""),
                "booksFinished" to parentStats.finished,
                "pagesRead" to parentStats.pages,
                "booksTotal" to parentBooks.size,
                "milestonesUnlocked" to parentMilestones.unlockedCount,
                "unlockedMilestoneIds" to parentMilestones.unlockedIds,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        )
        batch.commit().await()
        updateReaderLeaderboard(user.uid, user.displayName ?: "Reader", parentStats.finished, parentStats.pages)
        syncMilestonesLeaderboard(user.uid, user.displayName ?: "Reader", parentMilestones.unlockedCount)

        val kids = kidProfileRepository.getAll()
        kids.forEach { kid ->
            val kidBooks = bookRepository.getBooksForProfile(kid.id)
            val kidStats = computeStats(kidBooks)
            val kidMilestones = computeMilestones(kidBooks)
            userRef.collection("kids").document(kid.id.toString()).set(
                mapOf(
                    "localId" to kid.id,
                    "name" to kid.name,
                    "emoji" to kid.emoji,
                    "gender" to kid.gender,
                    "dateOfBirth" to kid.dateOfBirth,
                    "favoriteGenre" to kid.favoriteGenre,
                    "notes" to kid.notes,
                    "booksFinished" to kidStats.finished,
                    "pagesRead" to kidStats.pages,
                    "booksTotal" to kidBooks.size,
                    "milestonesUnlocked" to kidMilestones.unlockedCount,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
            syncKidLeaderboard(user.uid, kid, kidBooks)
            allBooks.filter { it.status == ReadingStatus.FINISHED }.forEach { updateGlobalReadingStats(it) }
        }
    }

    private suspend fun ensureUserDocument(user: FirebaseUser, displayName: String) {
        firestore.collection("users").document(user.uid).set(
            mapOf(
                "displayName" to displayName,
                "email" to (user.email ?: ""),
                "booksFinished" to 0,
                "pagesRead" to 0,
                "booksTotal" to 0,
                "milestonesUnlocked" to 0,
                "createdAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
        updateReaderLeaderboard(user.uid, displayName, 0, 0)
        syncMilestonesLeaderboard(user.uid, displayName, 0)
    }

    private suspend fun syncKidLeaderboard(parentUid: String, kid: KidProfile, books: List<Book>) {
        val stats = computeStats(books)
        firestore.collection("leaderboard_kids").document(kidLeaderboardId(parentUid, kid.id)).set(
            mapOf(
                "displayName" to kid.name,
                "emoji" to kid.emoji,
                "parentUid" to parentUid,
                "kidProfileId" to kid.id,
                "booksFinished" to stats.finished,
                "pagesRead" to stats.pages,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun updateReaderLeaderboard(uid: String, name: String, finished: Int, pages: Int) {
        firestore.collection("leaderboard").document(uid).set(
            mapOf(
                "displayName" to name,
                "booksFinished" to finished,
                "pagesRead" to pages,
                "isKidProfile" to false,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun syncMilestonesLeaderboard(uid: String, name: String, unlockedCount: Int) {
        firestore.collection("leaderboard_milestones").document(uid).set(
            mapOf(
                "displayName" to name,
                "milestonesUnlocked" to unlockedCount,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun updateGlobalReadingStats(book: Book) {
        val author = book.author.trim().takeIf { it.isNotBlank() } ?: return
        updateAggregate("leaderboard_authors", author, book.pageCount ?: 0)
        book.publisher?.trim()?.takeIf { it.isNotBlank() }?.let { publisher ->
            updateAggregate("leaderboard_publishers", publisher, book.pageCount ?: 0)
        }
        book.genre?.trim()?.takeIf { it.isNotBlank() }?.let { genre ->
            updateAggregate("leaderboard_genres", genre, book.pageCount ?: 0)
        }
    }

    private suspend fun updateAggregate(collection: String, name: String, pages: Int) {
        val slug = slugify(name)
        firestore.collection(collection).document(slug).set(
            mapOf(
                "name" to name,
                "booksFinished" to FieldValue.increment(1),
                "pagesRead" to FieldValue.increment(pages.toLong()),
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private suspend fun fetchReadersLeaderboard(limit: Long): List<LeaderboardEntry> {
        val snapshot = firestore.collection("leaderboard")
            .orderBy("booksFinished", Query.Direction.DESCENDING)
            .orderBy("pagesRead", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapIndexed { index, doc ->
            LeaderboardEntry(
                id = doc.id,
                displayName = doc.getString("displayName") ?: "Reader",
                primaryValue = doc.getLong("booksFinished")?.toInt() ?: 0,
                secondaryValue = doc.getLong("pagesRead")?.toInt() ?: 0,
                rank = index + 1,
            )
        }
    }

    private suspend fun fetchKidsLeaderboard(limit: Long): List<LeaderboardEntry> {
        val snapshot = firestore.collection("leaderboard_kids")
            .orderBy("booksFinished", Query.Direction.DESCENDING)
            .orderBy("pagesRead", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapIndexed { index, doc ->
            LeaderboardEntry(
                id = doc.id,
                displayName = doc.getString("displayName") ?: "Reader",
                primaryValue = doc.getLong("booksFinished")?.toInt() ?: 0,
                secondaryValue = doc.getLong("pagesRead")?.toInt() ?: 0,
                rank = index + 1,
                emoji = doc.getString("emoji"),
            )
        }
    }

    private suspend fun fetchMilestonesLeaderboard(limit: Long): List<LeaderboardEntry> {
        val snapshot = firestore.collection("leaderboard_milestones")
            .orderBy("milestonesUnlocked", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapIndexed { index, doc ->
            LeaderboardEntry(
                id = doc.id,
                displayName = doc.getString("displayName") ?: "Reader",
                primaryValue = doc.getLong("milestonesUnlocked")?.toInt() ?: 0,
                rank = index + 1,
            )
        }
    }

    private suspend fun fetchAggregateLeaderboard(
        collection: String,
        orderField: String,
        limit: Long,
    ): List<LeaderboardEntry> {
        val snapshot = firestore.collection(collection)
            .orderBy(orderField, Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapIndexed { index, doc ->
            LeaderboardEntry(
                id = doc.id,
                displayName = doc.getString("name") ?: doc.id,
                primaryValue = doc.getLong("booksFinished")?.toInt() ?: 0,
                secondaryValue = doc.getLong("pagesRead")?.toInt() ?: 0,
                rank = index + 1,
            )
        }
    }

    private fun computeStats(books: List<Book>): Stats {
        val finished = books.count { it.status == ReadingStatus.FINISHED }
        val pages = books.filter { it.status == ReadingStatus.FINISHED }.sumOf { it.pageCount ?: 0 }
        return Stats(finished, pages)
    }

    private fun computeMilestones(books: List<Book>): MilestoneStats {
        val snapshot = ReadingSnapshot(
            totalBooks = books.size,
            wantToRead = books.count { it.status == ReadingStatus.WANT_TO_READ },
            reading = books.count { it.status == ReadingStatus.READING },
            finished = books.count { it.status == ReadingStatus.FINISHED },
            pagesFinished = books.filter { it.status == ReadingStatus.FINISHED }.sumOf { it.pageCount ?: 0 },
            pagesInProgress = books.filter { it.status == ReadingStatus.READING }.sumOf { it.currentPage ?: 0 },
            ratedBooks = books.count { (it.rating ?: 0f) > 0f },
            longestFinishedPages = books.filter { it.status == ReadingStatus.FINISHED }
                .maxOfOrNull { it.pageCount ?: 0 } ?: 0,
        )
        val milestones = MilestoneEngine.compute(snapshot)
        return MilestoneStats(
            unlockedCount = MilestoneEngine.unlockedCount(milestones),
            unlockedIds = milestones.filter { it.isUnlocked }.map { it.id },
        )
    }

    private fun kidLeaderboardId(parentUid: String, kidId: Long) = "${parentUid}_kid_$kidId"

    private fun slugify(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "unknown" }

    private data class Stats(val finished: Int, val pages: Int)

    private data class MilestoneStats(val unlockedCount: Int, val unlockedIds: List<String>)

    private fun Book.toCloudMap(): Map<String, Any?> = mapOf(
        "localId" to id,
        "isbn" to isbn,
        "title" to title,
        "author" to author,
        "publisher" to publisher,
        "genre" to genre,
        "kidProfileId" to kidProfileId,
        "coverUrl" to coverUrl,
        "pageCount" to pageCount,
        "publishedYear" to publishedYear,
        "status" to status.name,
        "rating" to rating,
        "notes" to notes,
        "dateAdded" to dateAdded,
        "dateStarted" to dateStarted,
        "dateFinished" to dateFinished,
        "currentPage" to currentPage,
        "updatedAt" to System.currentTimeMillis(),
    )
}