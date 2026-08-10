package com.brk.booklogger.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.brk.booklogger.BookLogApplication
import com.brk.booklogger.data.audio.AppSound
import com.brk.booklogger.data.local.Book
import com.brk.booklogger.data.local.CompletedBook
import com.brk.booklogger.ui.components.AppTopBar
import com.brk.booklogger.ui.components.ScanHistoryDialog
import com.brk.booklogger.ui.screens.AddBookScreen
import com.brk.booklogger.ui.screens.BookDetailScreen
import com.brk.booklogger.ui.screens.KidProfileDetailScreen
import com.brk.booklogger.ui.screens.KidProfilesScreen
import com.brk.booklogger.ui.screens.LeaderboardScreen
import com.brk.booklogger.ui.screens.LibraryScreen
import com.brk.booklogger.ui.screens.LogCompletionScreen
import com.brk.booklogger.ui.screens.MilestonesScreen
import com.brk.booklogger.ui.screens.ReaderHomeScreen
import com.brk.booklogger.ui.screens.ScanScreen
import com.brk.booklogger.ui.screens.SettingsDrawerContent
import com.brk.booklogger.viewmodel.AddBookViewModel
import com.brk.booklogger.viewmodel.AuthViewModel
import com.brk.booklogger.viewmodel.BookDetailViewModel
import com.brk.booklogger.viewmodel.HouseholdViewModel
import com.brk.booklogger.viewmodel.KidsViewModel
import com.brk.booklogger.viewmodel.LeaderboardViewModel
import com.brk.booklogger.viewmodel.LibraryViewModel
import com.brk.booklogger.viewmodel.LogCompletionViewModel
import com.brk.booklogger.viewmodel.MilestonesViewModel
import kotlinx.coroutines.launch

object Routes {
    const val LIBRARY = "library"
    const val LEADERBOARD = "leaderboard"
    const val ACCOUNT = "account"
    const val MILESTONES = "milestones"
    const val ADD = "add"
    const val SCAN = "scan"
    const val LOG_COMPLETION = "log_completion"
    const val DETAIL = "detail/{bookId}"
    const val KID_PROFILES = "kid_profiles"
    const val KID_PROFILE_DETAIL = "kid_profile/{kidId}"

    fun detail(bookId: Long) = "detail/$bookId"
    fun kidProfileDetail(kidId: Long) = "kid_profile/$kidId"
    fun kidProfileNew() = "kid_profile/0"

    val bottomDestinations = setOf(LIBRARY, LEADERBOARD, ACCOUNT)
}

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BookLogNavHost(
    app: BookLogApplication,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.cloudRepository, app.guestPreferences),
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomDestinations
    val musicMuted by app.audioPreferences.musicMuted.collectAsStateWithLifecycle()

    val musicRoutes = setOf(Routes.LIBRARY, Routes.MILESTONES)
    LaunchedEffect(currentRoute, musicMuted) {
        if (!musicMuted && currentRoute in musicRoutes) {
            app.audioManager.startBackgroundMusic()
        } else {
            app.audioManager.stopBackgroundMusic()
        }
    }

    val tabs = listOf(
        BottomTab(Routes.LIBRARY, "Books", Icons.Default.MenuBook),
        BottomTab(Routes.LEADERBOARD, "Rank", Icons.Default.EmojiEvents),
        BottomTab(Routes.ACCOUNT, "Reader", Icons.Default.Person),
    )

    val activity = LocalContext.current as ComponentActivity
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val settingsOpen = drawerState.isOpen

    val kidsFactory = KidsViewModel.Factory(
        app.kidProfileRepository,
        app.activeKidPreferences,
        app.cloudRepository,
        app.readerBootstrap,
    )
    val sharedKidsVm: KidsViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = kidsFactory,
    )
    val sharedKidsState by sharedKidsVm.uiState.collectAsStateWithLifecycle()
    val householdVm: HouseholdViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = HouseholdViewModel.Factory(app.cloudRepository),
    )
    LaunchedEffect(sharedKidsState.kids) {
        sharedKidsVm.ensureDefaultKidFor(sharedKidsState.kids)
    }
    LaunchedEffect(Unit) {
        app.pullHouseholdIfLinked()
    }
    val activeReaderLabel =
        sharedKidsState.activeKid?.let { "${it.emoji} ${it.firstName}" } ?: "Reader"

    fun toggleSettingsDrawer() {
        scope.launch {
            if (drawerState.isOpen) drawerState.close() else drawerState.open()
        }
    }

    fun closeSettingsDrawer() {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                SettingsDrawerContent(
                    viewModel = authViewModel,
                    kidsViewModel = sharedKidsVm,
                    householdViewModel = householdVm,
                    audioPreferences = app.audioPreferences,
                    audioManager = app.audioManager,
                    onLeaderboardClick = {
                        closeSettingsDrawer()
                        navController.navigate(Routes.LEADERBOARD) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onKidProfilesClick = {
                        closeSettingsDrawer()
                        navController.navigate(Routes.KID_PROFILES)
                    },
                )
            }
        },
    ) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showBottomBar) {
                AppTopBar(
                    settingsOpen = settingsOpen,
                    onToggleSettings = { toggleSettingsDrawer() },
                    kids = sharedKidsState.kids,
                    activeKidId = sharedKidsState.activeKidId,
                    onSelectKid = sharedKidsVm::selectKid,
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LIBRARY) { entry ->
                val vm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        repository = app.repository,
                        rewardRepository = app.rewardRepository,
                        activeKidIdProvider = { app.activeKidPreferences.getActiveKidId() },
                    ),
                )
                val addedTitle = entry.savedStateHandle.get<String>("added_book_title")
                val updatedTitle = entry.savedStateHandle.get<String>("updated_book_title")
                LaunchedEffect(addedTitle) {
                    if (!addedTitle.isNullOrBlank()) {
                        entry.savedStateHandle.remove<String>("added_book_title")
                        app.audioManager.playSound(AppSound.BOOK_ADDED)
                        snackbarHostState.showSnackbar("ðŸŽ‰ \"$addedTitle\" added to your library!")
                        app.celebrateMilestonesForActiveProfile()
                    }
                }
                LaunchedEffect(updatedTitle) {
                    if (!updatedTitle.isNullOrBlank()) {
                        entry.savedStateHandle.remove<String>("updated_book_title")
                        app.audioManager.playSound(AppSound.BOOK_SAVED)
                        snackbarHostState.showSnackbar("âœ… \"$updatedTitle\" saved!")
                    }
                }
                val streakInfo by vm.streakInfo.collectAsStateWithLifecycle()
                LaunchedEffect(sharedKidsState.activeKidId) { vm.refreshActiveKid() }
                LibraryScreen(
                    viewModel = vm,
                    activeReaderLabel = activeReaderLabel,
                    streakInfo = streakInfo,
                    onBookClick = { navController.navigate(Routes.detail(it.id)) },
                    onManualAddClick = { navController.navigate(Routes.ADD) },
                    onScanClick = { navController.navigate(Routes.SCAN) },
                    onMilestonesClick = { navController.navigate(Routes.MILESTONES) },
                )
            }
            composable(Routes.LEADERBOARD) {
                val vm: LeaderboardViewModel = viewModel(
                    factory = LeaderboardViewModel.Factory(
                        app.cloudRepository,
                        app.kidProfileRepository,
                        app.rewardRepository,
                        app.repository,
                    ),
                )
                LeaderboardScreen(
                    viewModel = vm,
                    isSignedIn = authState.user != null,
                    currentUserId = authState.user?.uid,
                    onSignInClick = { toggleSettingsDrawer() },
                )
            }
            composable(Routes.ACCOUNT) {
                val libraryVm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        repository = app.repository,
                        rewardRepository = app.rewardRepository,
                        activeKidIdProvider = { app.activeKidPreferences.getActiveKidId() },
                    ),
                )
                val milestonesVm: MilestonesViewModel = viewModel(
                    factory = MilestonesViewModel.Factory(
                        app.repository,
                        app.rewardRepository,
                        app.activeKidPreferences,
                        app.milestonePreferences,
                        app.cloudRepository,
                    ),
                )
                LaunchedEffect(sharedKidsState.activeKidId) {
                    libraryVm.refreshActiveKid()
                    milestonesVm.refreshActiveKid()
                    milestonesVm.refreshScanCount()
                }
                val streakInfo by libraryVm.streakInfo.collectAsStateWithLifecycle()
                ReaderHomeScreen(
                    kidsViewModel = sharedKidsVm,
                    libraryViewModel = libraryVm,
                    milestonesViewModel = milestonesVm,
                    streakInfo = streakInfo,
                    onManageReaders = { navController.navigate(Routes.KID_PROFILES) },
                    onMilestonesClick = { navController.navigate(Routes.MILESTONES) },
                    onLibraryClick = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.KID_PROFILES) {
                val kidsVm: KidsViewModel = viewModel(factory = kidsFactory)
                LaunchedEffect(Unit) { kidsVm.refreshActiveKid() }
                KidProfilesScreen(
                    viewModel = kidsVm,
                    onBack = { navController.popBackStack() },
                    onAddKid = { navController.navigate(Routes.kidProfileNew()) },
                    onEditKid = { id -> navController.navigate(Routes.kidProfileDetail(id)) },
                )
            }
            composable(
                route = Routes.KID_PROFILE_DETAIL,
                arguments = listOf(navArgument("kidId") { type = NavType.LongType }),
            ) { entry ->
                val kidId = entry.arguments?.getLong("kidId") ?: return@composable
                val kidsVm: KidsViewModel = viewModel(factory = kidsFactory)
                KidProfileDetailScreen(
                    kidId = kidId,
                    viewModel = kidsVm,
                    repository = app.kidProfileRepository,
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(Routes.MILESTONES) {
                val vm: MilestonesViewModel = viewModel(
                    factory = MilestonesViewModel.Factory(
                        app.repository,
                        app.rewardRepository,
                        app.activeKidPreferences,
                        app.milestonePreferences,
                        app.cloudRepository,
                    ),
                )
                LaunchedEffect(Unit) { vm.refreshScanCount() }
                LaunchedEffect(sharedKidsState.activeKidId) { vm.refreshActiveKid() }
                MilestonesScreen(
                    viewModel = vm,
                    audioManager = app.audioManager,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD) { entry ->
                val vm: AddBookViewModel = viewModel(
                    factory = AddBookViewModel.Factory(
                        repository = app.repository,
                        activeKidIdProvider = { app.activeKidPreferences.getActiveKidId() },
                        onBookSaved = { book -> app.syncBookToCloud(book) },
                    ),
                )
                LaunchedEffect(sharedKidsState.activeKidId) {
                    vm.refreshSelectedKid()
                }
                val scannedIsbn by entry.savedStateHandle
                    .getStateFlow<String?>("scanned_isbn", null)
                    .collectAsStateWithLifecycle()
                AddBookScreen(
                    viewModel = vm,
                    activeReaderLabel = activeReaderLabel,
                    onBack = { navController.popBackStack() },
                    onSaved = { book ->
                        if ((book.pageCount ?: 0) > 0) {
                            navController.navigateToLogCompletion(book)
                        } else {
                            runCatching {
                                navController.getBackStackEntry(Routes.LIBRARY)
                                    .savedStateHandle["added_book_title"] = book.title
                            }
                            navController.popBackStack()
                        }
                    },
                    scannedIsbn = scannedIsbn,
                )
            }
            composable(Routes.SCAN) {
                val scanScope = rememberCoroutineScope()
                var pendingBook by remember { mutableStateOf<Book?>(null) }
                var scanHistory by remember { mutableStateOf<List<CompletedBook>>(emptyList()) }
                var showHistoryDialog by remember { mutableStateOf(false) }

                if (showHistoryDialog && pendingBook != null) {
                    ScanHistoryDialog(
                        title = pendingBook!!.title,
                        author = pendingBook!!.author,
                        history = scanHistory,
                        onLogAnotherRead = {
                            showHistoryDialog = false
                            navController.navigateToLogCompletion(pendingBook!!)
                            pendingBook = null
                        },
                        onDismiss = {
                            showHistoryDialog = false
                            pendingBook = null
                        },
                    )
                }

                ScanScreen(
                    onBack = { navController.popBackStack() },
                    onIsbnScanned = { isbn ->
                        scanScope.launch {
                            app.audioManager.playSound(AppSound.SCAN_SUCCESS)
                            app.recordBookScanned()
                            val kidId = app.activeKidPreferences.getActiveKidId()
                            app.repository.lookupMetadataByIsbn(isbn)
                                .onSuccess { metadata ->
                                    val localBook = app.repository.findLocalBookByIsbn(isbn, kidId)
                                    val history = if (!isbn.isBlank()) {
                                        app.rewardRepository.getCompletionHistory(isbn, kidId)
                                    } else {
                                        emptyList()
                                    }
                                    val merged = (localBook ?: metadata).copy(
                                        kidProfileId = kidId,
                                        title = metadata.title.ifBlank { localBook?.title.orEmpty() },
                                        author = metadata.author.ifBlank { localBook?.author.orEmpty() },
                                        pageCount = metadata.pageCount ?: localBook?.pageCount,
                                        coverUrl = metadata.coverUrl ?: localBook?.coverUrl,
                                        isbn = metadata.isbn ?: localBook?.isbn,
                                    )
                                    if (merged.title.isNotBlank() && merged.author.isNotBlank()) {
                                        if (history.isNotEmpty()) {
                                            pendingBook = merged
                                            scanHistory = history
                                            showHistoryDialog = true
                                        } else {
                                            navController.navigateToLogCompletion(merged)
                                        }
                                    } else {
                                        navController.navigate(Routes.ADD) {
                                            popUpTo(Routes.SCAN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                        navController.currentBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("scanned_isbn", isbn)
                                    }
                                }
                                .onFailure {
                                    navController.navigate(Routes.ADD) {
                                        popUpTo(Routes.SCAN) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    navController.currentBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("scanned_isbn", isbn)
                                }
                        }
                    },
                )
            }
            composable(Routes.LOG_COMPLETION) { entry ->
                val title by entry.savedStateHandle
                    .getStateFlow("completion_title", "")
                    .collectAsStateWithLifecycle()
                val book = entry.savedStateHandle.toCompletionBook(title)
                if (book == null) return@composable
                val kidId = app.activeKidPreferences.getActiveKidId()
                val vm: LogCompletionViewModel = viewModel(
                    key = "log_completion_${book.isbn}_${book.id}",
                    factory = LogCompletionViewModel.Factory(
                        initialBook = book,
                        kidId = kidId,
                        bookRepository = app.repository,
                        rewardRepository = app.rewardRepository,
                        onBookSynced = { saved -> app.syncBookToCloud(saved) },
                    ),
                )
                LogCompletionScreen(
                    viewModel = vm,
                    activeReaderLabel = activeReaderLabel,
                    onBack = { navController.popBackStack() },
                    onSuccess = { title, rewardCents ->
                        app.audioManager.playSound(AppSound.BOOK_ADDED)
                        app.celebrateMilestonesForActiveProfile()
                        runCatching {
                            navController.getBackStackEntry(Routes.LIBRARY)
                                .savedStateHandle["added_book_title"] = "$title (+${com.brk.booklogger.data.repository.RewardRepository.formatCents(rewardCents)})"
                        }
                        navController.popBackStack(Routes.LIBRARY, inclusive = false)
                    },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
            ) { entry ->
                val bookId = entry.arguments?.getLong("bookId") ?: return@composable
                val vm: BookDetailViewModel = viewModel(
                    factory = BookDetailViewModel.Factory(
                        repository = app.repository,
                        rewardRepository = app.rewardRepository,
                        milestoneCelebrationCoordinator = app.milestoneCelebrationCoordinator,
                        bookId = bookId,
                        activeKidIdProvider = { app.activeKidPreferences.getActiveKidId() },
                        onBookUpdated = { book -> app.syncBookToCloud(book) },
                    ),
                )
                BookDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { title ->
                        runCatching {
                            navController.getBackStackEntry(Routes.LIBRARY)
                                .savedStateHandle["updated_book_title"] = title
                        }
                    },
                )
            }
        }
    }
    } // ModalNavigationDrawer
}

private fun NavHostController.navigateToLogCompletion(book: Book) {
    navigate(Routes.LOG_COMPLETION)
    runCatching {
        getBackStackEntry(Routes.LOG_COMPLETION).savedStateHandle.putCompletionBook(book)
    }
}

private fun androidx.lifecycle.SavedStateHandle.putCompletionBook(book: Book) {
    set("completion_isbn", book.isbn)
    set("completion_title", book.title)
    set("completion_author", book.author)
    set("completion_pages", book.pageCount ?: 0)
    set("completion_cover_url", book.coverUrl)
    set("completion_book_id", book.id)
}

private fun androidx.lifecycle.SavedStateHandle.toCompletionBook(title: String): Book? {
    if (title.isBlank()) return null
    return Book(
        id = get<Long>("completion_book_id") ?: 0L,
        isbn = get<String>("completion_isbn"),
        title = title,
        author = get<String>("completion_author") ?: "",
        pageCount = get<Int>("completion_pages"),
        coverUrl = get<String>("completion_cover_url"),
    )
}