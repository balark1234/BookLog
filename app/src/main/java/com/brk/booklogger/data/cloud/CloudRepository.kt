package com.brk.booklogger.data.cloud

import com.brk.booklogger.data.local.Book
import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.data.local.ReaderProfileType
import com.brk.booklogger.data.local.ReadingStatus
import com.brk.booklogger.data.milestones.Milestone
import com.brk.booklogger.data.milestones.MilestoneEngine
import com.brk.booklogger.data.milestones.ReadingSnapshot
import com.brk.booklogger.data.profiles.HouseholdPreferences
import com.brk.booklogger.data.repository.BookRepository
import com.brk.booklogger.data.repository.KidProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class CloudRepository(
    private val bookRepository: BookRepository,
    private val kidProfileRepository: KidProfileRepository,
    private val householdPreferences: HouseholdPreferences,
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
        restoreHouseholdFromUser()
        pullLibraryFromCloud()
        syncLocalBooksToCloud()
    }.map { }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Google sign-in succeeded but user is missing")
        val displayName = user.displayName?.trim().orEmpty().ifBlank { "Reader" }
        ensureUserDocument(user, displayName)
        restoreHouseholdFromUser()
        pullLibraryFromCloud()
        syncLocalBooksToCloud()
    }.map { }

    fun signOut() {
        householdPreferences.setHouseholdId(null)
        householdPreferences.setInviteCode(null)
        auth.signOut()
    }

    suspend fun syncLocalBooksToCloud(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user)
        if (householdId != null) {
            syncToHousehold(user, householdId)
        } else {
            syncAllProfiles(user)
        }
    }

    suspend fun syncBook(book: Book): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user)
        val ensured = ensureBookCloudId(book)
        if (householdId != null) {
            val readerCloudId = ensured.kidProfileId?.let { kidProfileRepository.getById(it)?.cloudId }
            firestore.collection(COL_HOUSEHOLDS).document(householdId)
                .collection(COL_BOOKS).document(ensured.cloudId!!)
                .set(ensured.toCloudMap(readerCloudId, user.uid), SetOptions.merge())
                .await()
            syncToHousehold(user, householdId)
        } else {
            firestore.collection(COL_USERS).document(user.uid)
                .collection(COL_BOOKS).document(ensured.cloudId ?: ensured.id.toString())
                .set(ensured.toCloudMap(null, user.uid), SetOptions.merge())
                .await()
            syncAllProfiles(user)
        }
        if (ensured.status == ReadingStatus.FINISHED) {
            updateGlobalReadingStats(ensured)
        }
    }

    suspend fun syncKidProfiles(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user)
        if (householdId != null) syncToHousehold(user, householdId) else syncAllProfiles(user)
    }

    suspend fun addKidProfile(profile: KidProfile): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val ensured = ensureReaderCloudId(profile)
        val householdId = resolveHouseholdId(user)
        if (householdId != null) {
            writeHouseholdReader(householdId, ensured)
            syncKidLeaderboard(householdId, ensured, emptyList())
        } else {
            writeUserReader(user.uid, ensured)
            syncKidLeaderboard(user.uid, ensured, emptyList())
        }
    }

    suspend fun removeKidProfile(profile: KidProfile): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user)
        val docId = profile.cloudId ?: profile.id.toString()
        if (householdId != null) {
            firestore.collection(COL_HOUSEHOLDS).document(householdId)
                .collection(COL_READERS).document(docId).delete().await()
            firestore.collection("leaderboard_kids").document(kidLeaderboardId(householdId, profile.id))
                .delete().await()
        } else {
            firestore.collection(COL_USERS).document(user.uid)
                .collection(COL_KIDS).document(profile.id.toString()).delete().await()
            firestore.collection("leaderboard_kids").document(kidLeaderboardId(user.uid, profile.id))
                .delete().await()
        }
    }

    // --- Household / partner linking ---

    suspend fun fetchHousehold(): Result<HouseholdInfo> = runCatching {
        val user = auth.currentUser ?: error("Sign in to manage a household")
        val householdId = resolveHouseholdId(user) ?: return@runCatching HouseholdInfo(isLinked = false)
        val doc = firestore.collection(COL_HOUSEHOLDS).document(householdId).get().await()
        if (!doc.exists()) {
            householdPreferences.setHouseholdId(null)
            householdPreferences.setInviteCode(null)
            return@runCatching HouseholdInfo(isLinked = false)
        }
        @Suppress("UNCHECKED_CAST")
        val memberUids = (doc.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val names = memberUids.map { uid ->
            firestore.collection(COL_USERS).document(uid).get().await()
                .getString("displayName") ?: "Partner"
        }
        val code = doc.getString("inviteCode").orEmpty()
        householdPreferences.setInviteCode(code)
        HouseholdInfo(
            id = householdId,
            inviteCode = code,
            memberUids = memberUids,
            memberNames = names,
            createdBy = doc.getString("createdBy").orEmpty(),
            isLinked = true,
        )
    }

    suspend fun createHousehold(): Result<HouseholdInfo> = runCatching {
        val user = auth.currentUser ?: error("Sign in to create a household")
        val existing = resolveHouseholdId(user)
        if (existing != null) {
            return@runCatching fetchHousehold().getOrThrow()
        }
        val householdId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()
        val now = System.currentTimeMillis()
        firestore.collection(COL_HOUSEHOLDS).document(householdId).set(
            mapOf(
                "memberUids" to listOf(user.uid),
                "inviteCode" to inviteCode,
                "createdBy" to user.uid,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        ).await()
        // Index for join lookup
        firestore.collection(COL_INVITES).document(inviteCode).set(
            mapOf(
                "householdId" to householdId,
                "createdBy" to user.uid,
                "createdAt" to now,
            ),
        ).await()
        firestore.collection(COL_USERS).document(user.uid).set(
            mapOf("householdId" to householdId, "updatedAt" to now),
            SetOptions.merge(),
        ).await()
        householdPreferences.setHouseholdId(householdId)
        householdPreferences.setInviteCode(inviteCode)
        // Push current library into household
        syncToHousehold(user, householdId)
        HouseholdInfo(
            id = householdId,
            inviteCode = inviteCode,
            memberUids = listOf(user.uid),
            memberNames = listOf(user.displayName ?: "You"),
            createdBy = user.uid,
            isLinked = true,
        )
    }

    suspend fun joinHousehold(rawCode: String): Result<HouseholdInfo> = runCatching {
        val user = auth.currentUser ?: error("Sign in to join a household")
        if (resolveHouseholdId(user) != null) {
            error("You are already in a household. Leave it before joining another.")
        }
        val code = rawCode.trim().uppercase()
        if (code.length < 4) error("Enter a valid invite code")
        val invite = firestore.collection(COL_INVITES).document(code).get().await()
        if (!invite.exists()) error("Invite code not found")
        val householdId = invite.getString("householdId") ?: error("Invalid invite")
        val householdRef = firestore.collection(COL_HOUSEHOLDS).document(householdId)
        val household = householdRef.get().await()
        if (!household.exists()) error("Household no longer exists")
        @Suppress("UNCHECKED_CAST")
        val members = (household.get("memberUids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            .toMutableList()
        if (members.contains(user.uid)) {
            // Already a member — just restore local link
        } else {
            if (members.size >= MAX_HOUSEHOLD_MEMBERS) {
                error("This household is full (max $MAX_HOUSEHOLD_MEMBERS partners)")
            }
            members.add(user.uid)
            householdRef.set(
                mapOf(
                    "memberUids" to members,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
        }
        firestore.collection(COL_USERS).document(user.uid).set(
            mapOf("householdId" to householdId, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge(),
        ).await()
        householdPreferences.setHouseholdId(householdId)
        householdPreferences.setInviteCode(household.getString("inviteCode") ?: code)
        // Merge this device's library up, then pull shared library
        syncToHousehold(user, householdId)
        pullHouseholdLibrary()
        fetchHousehold().getOrThrow()
    }

    suspend fun leaveHousehold(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val householdId = resolveHouseholdId(user) ?: return@runCatching
        val householdRef = firestore.collection(COL_HOUSEHOLDS).document(householdId)
        val household = householdRef.get().await()
        if (household.exists()) {
            @Suppress("UNCHECKED_CAST")
            val members = (household.get("memberUids") as? List<*>)?.mapNotNull { it as? String }
                .orEmpty()
                .filterNot { it == user.uid }
            if (members.isEmpty()) {
                // Last member — remove invite index and household
                household.getString("inviteCode")?.let { code ->
                    firestore.collection(COL_INVITES).document(code).delete().await()
                }
                householdRef.delete().await()
            } else {
                householdRef.set(
                    mapOf(
                        "memberUids" to members,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                    SetOptions.merge(),
                ).await()
            }
        }
        firestore.collection(COL_USERS).document(user.uid).set(
            mapOf("householdId" to null, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge(),
        ).await()
        householdPreferences.setHouseholdId(null)
        householdPreferences.setInviteCode(null)
    }

    suspend fun regenerateInviteCode(): Result<String> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val householdId = resolveHouseholdId(user) ?: error("Not in a household")
        val householdRef = firestore.collection(COL_HOUSEHOLDS).document(householdId)
        val household = householdRef.get().await()
        if (household.getString("createdBy") != user.uid) {
            error("Only the household creator can regenerate the code")
        }
        val oldCode = household.getString("inviteCode")
        val newCode = generateInviteCode()
        if (!oldCode.isNullOrBlank()) {
            firestore.collection(COL_INVITES).document(oldCode).delete().await()
        }
        householdRef.set(
            mapOf("inviteCode" to newCode, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge(),
        ).await()
        firestore.collection(COL_INVITES).document(newCode).set(
            mapOf(
                "householdId" to householdId,
                "createdBy" to user.uid,
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
        householdPreferences.setInviteCode(newCode)
        newCode
    }

    /**
     * Pull library for this account: household (if linked) or solo `users/{uid}` tree.
     * Book covers are resolved on-device from Open Library (never from Firebase).
     */
    suspend fun pullLibraryFromCloud(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user)
        if (householdId != null) {
            pullHouseholdLibrary().getOrThrow()
        } else {
            pullSoloLibrary(user).getOrThrow()
        }
    }

    /** Pull shared readers + books into local Room. */
    suspend fun pullHouseholdLibrary(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        val householdId = resolveHouseholdId(user) ?: return@runCatching
        val readersSnap = firestore.collection(COL_HOUSEHOLDS).document(householdId)
            .collection(COL_READERS).get().await()
        val cloudIdToLocalId = importReadersFromDocs(readersSnap.documents)

        val booksSnap = firestore.collection(COL_HOUSEHOLDS).document(householdId)
            .collection(COL_BOOKS).get().await()
        importBooksFromDocs(booksSnap.documents, cloudIdToLocalId)
    }

    /** Pull solo account readers + books (new phone / reinstall). */
    private suspend fun pullSoloLibrary(user: FirebaseUser): Result<Unit> = runCatching {
        val kidsSnap = firestore.collection(COL_USERS).document(user.uid)
            .collection(COL_KIDS).get().await()
        val cloudIdToLocalId = importReadersFromDocs(kidsSnap.documents)

        val booksSnap = firestore.collection(COL_USERS).document(user.uid)
            .collection(COL_BOOKS).get().await()
        importBooksFromDocs(booksSnap.documents, cloudIdToLocalId)
    }

    private suspend fun importReadersFromDocs(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
    ): MutableMap<String, Long> {
        val cloudIdToLocalId = mutableMapOf<String, Long>()
        for (doc in documents) {
            val cloudId = doc.getString("cloudId") ?: doc.id
            val existing = kidProfileRepository.getByCloudId(cloudId)
            val profile = KidProfile(
                id = existing?.id ?: 0L,
                name = doc.getString("name") ?: "Reader",
                emoji = doc.getString("emoji") ?: "📚",
                gender = doc.getString("gender") ?: "PREFER_NOT_TO_SAY",
                dateOfBirth = doc.getLong("dateOfBirth"),
                favoriteGenre = doc.getString("favoriteGenre").orEmpty(),
                notes = doc.getString("notes").orEmpty(),
                profileType = doc.getString("profileType") ?: ReaderProfileType.CHILD.name,
                cloudId = cloudId,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            )
            val saved = kidProfileRepository.save(profile)
            cloudIdToLocalId[cloudId] = saved.id
            doc.getLong("localId")?.let { cloudIdToLocalId["local:$it"] = saved.id }
        }
        return cloudIdToLocalId
    }

    private suspend fun importBooksFromDocs(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        cloudIdToLocalId: Map<String, Long>,
    ) {
        for (doc in documents) {
            val cloudId = doc.getString("cloudId") ?: doc.id
            val readerCloudId = doc.getString("readerCloudId")
            val localReaderId = readerCloudId?.let { cloudIdToLocalId[it] }
                ?: doc.getLong("kidProfileId")?.let { cloudIdToLocalId["local:$it"] }
                ?: doc.getLong("kidProfileId")
            val statusName = doc.getString("status") ?: ReadingStatus.WANT_TO_READ.name
            val isbn = doc.getString("isbn")
            val title = doc.getString("title") ?: "Untitled"
            val author = doc.getString("author") ?: "Unknown"
            // Prefer coverUrl from Firebase; if missing, resolve via Open Library
            val coverFromCloud = doc.getString("coverUrl")?.takeIf { it.isNotBlank() }
            val coverUrl = coverFromCloud
                ?: bookRepository.resolveCoverFromCatalog(
                    isbn = isbn,
                    title = title,
                    author = author,
                )
            val book = Book(
                id = 0,
                isbn = isbn,
                title = title,
                author = author,
                coverUrl = coverUrl,
                pageCount = doc.getLong("pageCount")?.toInt(),
                publishedYear = doc.getString("publishedYear"),
                description = doc.getString("description"),
                publisher = doc.getString("publisher"),
                genre = doc.getString("genre"),
                kidProfileId = localReaderId,
                cloudId = cloudId,
                status = runCatching { ReadingStatus.valueOf(statusName) }
                    .getOrDefault(ReadingStatus.WANT_TO_READ),
                rating = doc.getDouble("rating")?.toFloat(),
                notes = doc.getString("notes").orEmpty(),
                dateAdded = doc.getLong("dateAdded") ?: System.currentTimeMillis(),
                dateStarted = doc.getLong("dateStarted"),
                dateFinished = doc.getLong("dateFinished"),
                currentPage = doc.getLong("currentPage")?.toInt(),
            )
            bookRepository.upsertFromCloud(book)
        }
    }

    suspend fun syncMilestones(snapshot: ReadingSnapshot, milestones: List<Milestone>): Result<Unit> =
        runCatching {
            val user = auth.currentUser ?: return@runCatching
            val unlocked = milestones.filter { it.isUnlocked }.map { it.id }
            val unlockedCount = unlocked.size
            firestore.collection(COL_USERS).document(user.uid).set(
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
        val doc = firestore.collection(COL_USERS).document(user.uid).get().await()
        CloudUserProfile(
            uid = user.uid,
            displayName = doc.getString("displayName") ?: user.displayName ?: "Reader",
            email = user.email ?: "",
            booksFinished = doc.getLong("booksFinished")?.toInt() ?: 0,
            pagesRead = doc.getLong("pagesRead")?.toInt() ?: 0,
            booksTotal = doc.getLong("booksTotal")?.toInt() ?: 0,
            milestonesUnlocked = doc.getLong("milestonesUnlocked")?.toInt() ?: 0,
            householdId = doc.getString("householdId") ?: householdPreferences.getHouseholdId(),
        )
    }

    suspend fun fetchKidProfiles(): Result<List<CloudKidProfile>> = runCatching {
        val localKids = kidProfileRepository.getAll()
        localKids.map { kid ->
            val books = bookRepository.getBooksForProfile(kid.id)
            val stats = computeStats(books)
            CloudKidProfile(
                localId = kid.id,
                name = kid.name,
                emoji = kid.emoji,
                booksFinished = stats.finished,
                pagesRead = stats.pages,
                milestonesUnlocked = 0,
            )
        }
    }

    private suspend fun restoreHouseholdFromUser() {
        val user = auth.currentUser ?: return
        val doc = firestore.collection(COL_USERS).document(user.uid).get().await()
        val householdId = doc.getString("householdId")
        householdPreferences.setHouseholdId(householdId)
        if (householdId != null) {
            val household = firestore.collection(COL_HOUSEHOLDS).document(householdId).get().await()
            householdPreferences.setInviteCode(household.getString("inviteCode"))
        } else {
            householdPreferences.setInviteCode(null)
        }
    }

    private suspend fun resolveHouseholdId(user: FirebaseUser): String? {
        householdPreferences.getHouseholdId()?.let { return it }
        val doc = firestore.collection(COL_USERS).document(user.uid).get().await()
        val id = doc.getString("householdId")
        if (id != null) householdPreferences.setHouseholdId(id)
        return id
    }

    private suspend fun syncToHousehold(user: FirebaseUser, householdId: String) {
        val readers = kidProfileRepository.getAll()
        readers.forEach { reader ->
            val ensured = ensureReaderCloudId(reader)
            writeHouseholdReader(householdId, ensured)
            val books = bookRepository.getBooksForProfile(ensured.id)
            val stats = computeStats(books)
            // Update stats on reader doc
            firestore.collection(COL_HOUSEHOLDS).document(householdId)
                .collection(COL_READERS).document(ensured.cloudId!!).set(
                    mapOf(
                        "booksFinished" to stats.finished,
                        "pagesRead" to stats.pages,
                        "booksTotal" to books.size,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                    SetOptions.merge(),
                ).await()
            if (ensured.readerType == ReaderProfileType.CHILD) {
                syncKidLeaderboard(householdId, ensured, books)
            }
            books.forEach { book ->
                val b = ensureBookCloudId(book)
                firestore.collection(COL_HOUSEHOLDS).document(householdId)
                    .collection(COL_BOOKS).document(b.cloudId!!)
                    .set(b.toCloudMap(ensured.cloudId, user.uid), SetOptions.merge())
                    .await()
            }
        }
        // Account-level summary for the signed-in adult leaderboard
        val allBooks = bookRepository.getAllBooks()
        val stats = computeStats(allBooks)
        firestore.collection(COL_USERS).document(user.uid).set(
            mapOf(
                "displayName" to (user.displayName ?: "Reader"),
                "email" to (user.email ?: ""),
                "householdId" to householdId,
                "booksFinished" to stats.finished,
                "pagesRead" to stats.pages,
                "booksTotal" to allBooks.size,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
        updateReaderLeaderboard(user.uid, user.displayName ?: "Reader", stats.finished, stats.pages)
    }

    private suspend fun syncAllProfiles(user: FirebaseUser) {
        val userRef = firestore.collection(COL_USERS).document(user.uid)
        val allBooks = bookRepository.getAllBooks()
        val readers = kidProfileRepository.getAll()

        // Prefer adult profiles for account-level stats; fall back to all books
        val adultIds = readers.filter { it.isAdult }.map { it.id }.toSet()
        val accountBooks = if (adultIds.isNotEmpty()) {
            allBooks.filter { it.kidProfileId in adultIds }
        } else {
            allBooks
        }
        val accountStats = computeStats(accountBooks)
        val accountMilestones = computeMilestones(accountBooks)
        userRef.set(
            mapOf(
                "displayName" to (user.displayName ?: "Reader"),
                "email" to (user.email ?: ""),
                "booksFinished" to accountStats.finished,
                "pagesRead" to accountStats.pages,
                "booksTotal" to accountBooks.size,
                "milestonesUnlocked" to accountMilestones.unlockedCount,
                "unlockedMilestoneIds" to accountMilestones.unlockedIds,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
        updateReaderLeaderboard(user.uid, user.displayName ?: "Reader", accountStats.finished, accountStats.pages)
        syncMilestonesLeaderboard(user.uid, user.displayName ?: "Reader", accountMilestones.unlockedCount)

        readers.forEach { reader ->
            val ensured = ensureReaderCloudId(reader)
            val kidBooks = bookRepository.getBooksForProfile(ensured.id)
            val kidStats = computeStats(kidBooks)
            val kidMilestones = computeMilestones(kidBooks)
            writeUserReader(user.uid, ensured, kidStats, kidMilestones, kidBooks.size)
            kidBooks.forEach { book ->
                val b = ensureBookCloudId(book)
                userRef.collection(COL_BOOKS).document(b.cloudId ?: b.id.toString())
                    .set(b.toCloudMap(ensured.cloudId, user.uid), SetOptions.merge())
                    .await()
            }
            if (ensured.readerType == ReaderProfileType.CHILD) {
                syncKidLeaderboard(user.uid, ensured, kidBooks)
            }
        }
        allBooks.filter { it.status == ReadingStatus.FINISHED }.forEach { updateGlobalReadingStats(it) }
    }

    private suspend fun writeHouseholdReader(householdId: String, profile: KidProfile) {
        val id = profile.cloudId ?: return
        firestore.collection(COL_HOUSEHOLDS).document(householdId)
            .collection(COL_READERS).document(id).set(profile.toCloudMap(), SetOptions.merge())
            .await()
    }

    private suspend fun writeUserReader(
        uid: String,
        profile: KidProfile,
        stats: Stats? = null,
        milestones: MilestoneStats? = null,
        booksTotal: Int = 0,
    ) {
        val base = profile.toCloudMap().toMutableMap()
        if (stats != null) {
            base["booksFinished"] = stats.finished
            base["pagesRead"] = stats.pages
            base["booksTotal"] = booksTotal
        }
        if (milestones != null) {
            base["milestonesUnlocked"] = milestones.unlockedCount
        }
        // Keep legacy localId doc id for solo accounts for compatibility
        firestore.collection(COL_USERS).document(uid)
            .collection(COL_KIDS).document(profile.id.toString())
            .set(base, SetOptions.merge())
            .await()
    }

    private suspend fun ensureUserDocument(user: FirebaseUser, displayName: String) {
        firestore.collection(COL_USERS).document(user.uid).set(
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

    private suspend fun ensureBookCloudId(book: Book): Book = bookRepository.ensureCloudId(book)

    private suspend fun ensureReaderCloudId(profile: KidProfile): KidProfile {
        if (!profile.cloudId.isNullOrBlank()) return profile
        return kidProfileRepository.save(profile.copy(cloudId = UUID.randomUUID().toString()))
    }

    private suspend fun syncKidLeaderboard(scopeId: String, kid: KidProfile, books: List<Book>) {
        val stats = computeStats(books)
        firestore.collection("leaderboard_kids").document(kidLeaderboardId(scopeId, kid.id)).set(
            mapOf(
                "displayName" to kid.name,
                "emoji" to kid.emoji,
                "parentUid" to scopeId,
                "kidProfileId" to kid.id,
                "profileType" to kid.profileType,
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

    private fun kidLeaderboardId(scopeId: String, kidId: Long) = "${scopeId}_kid_$kidId"

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            repeat(6) { append(alphabet[Random.nextInt(alphabet.length)]) }
        }
    }

    private fun slugify(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "unknown" }

    private data class Stats(val finished: Int, val pages: Int)

    private data class MilestoneStats(val unlockedCount: Int, val unlockedIds: List<String>)

    private fun KidProfile.toCloudMap(): Map<String, Any?> = mapOf(
        "localId" to id,
        "cloudId" to cloudId,
        "name" to name,
        "emoji" to emoji,
        "gender" to gender,
        "dateOfBirth" to dateOfBirth,
        "favoriteGenre" to favoriteGenre,
        "notes" to notes,
        "profileType" to profileType,
        "createdAt" to createdAt,
        "updatedAt" to System.currentTimeMillis(),
    )

    /**
     * Book metadata + who read it + cover URL text (not the image file).
     * Image bytes are still loaded by Coil from the URL / Open Library on each device.
     */
    private fun Book.toCloudMap(readerCloudId: String?, editedByUid: String?): Map<String, Any?> = mapOf(
        "localId" to id,
        "cloudId" to cloudId,
        "isbn" to isbn,
        "title" to title,
        "author" to author,
        "publisher" to publisher,
        "genre" to genre,
        "description" to description,
        "kidProfileId" to kidProfileId,
        "readerCloudId" to readerCloudId,
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
        "lastEditedByUid" to editedByUid,
        "updatedAt" to System.currentTimeMillis(),
    )

    companion object {
        private const val COL_USERS = "users"
        private const val COL_BOOKS = "books"
        private const val COL_KIDS = "kids"
        private const val COL_HOUSEHOLDS = "households"
        private const val COL_READERS = "readers"
        private const val COL_INVITES = "household_invites"
        private const val MAX_HOUSEHOLD_MEMBERS = 2
    }
}
