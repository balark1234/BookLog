package com.booklog.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.booklog.app.BookLogApplication
import com.booklog.app.data.audio.AppSound
import com.booklog.app.ui.components.ActiveReaderSelector
import kotlinx.coroutines.launch
import com.booklog.app.ui.screens.AddBookScreen
import com.booklog.app.ui.screens.BookDetailScreen
import com.booklog.app.ui.screens.LeaderboardScreen
import com.booklog.app.ui.screens.LibraryScreen
import com.booklog.app.ui.screens.ProfileScreen
import com.booklog.app.ui.screens.ScanScreen
import com.booklog.app.ui.screens.KidProfileDetailScreen
import com.booklog.app.ui.screens.KidProfilesScreen
import com.booklog.app.ui.screens.MilestonesScreen
import com.booklog.app.viewmodel.AddBookViewModel
import com.booklog.app.viewmodel.AuthViewModel
import com.booklog.app.viewmodel.BookDetailViewModel
import com.booklog.app.viewmodel.KidsViewModel
import com.booklog.app.viewmodel.LeaderboardViewModel
import com.booklog.app.viewmodel.LibraryViewModel
import com.booklog.app.viewmodel.MilestonesViewModel

object Routes {
    const val LIBRARY = "library"
    const val LEADERBOARD = "leaderboard"
    const val ACCOUNT = "account"
    const val MILESTONES = "milestones"
    const val ADD = "add"
    const val SCAN = "scan"
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
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(app.cloudRepository))
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
        BottomTab(Routes.ACCOUNT, "Me", Icons.Default.Person),
    )

    val activity = LocalContext.current as ComponentActivity
    val sharedKidsVm: KidsViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = KidsViewModel.Factory(
            app.kidProfileRepository,
            app.activeKidPreferences,
            app.cloudRepository,
        ),
    )
    val sharedKidsState by sharedKidsVm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(sharedKidsState.kids) {
        sharedKidsVm.ensureDefaultKidFor(sharedKidsState.kids)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showBottomBar) {
                ActiveReaderSelector(
                    kids = sharedKidsState.kids,
                    activeKidId = sharedKidsState.activeKidId,
                    onSelectKid = sharedKidsVm::selectKid,
                    compact = true,
                    title = "Reading as",
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
                        snackbarHostState.showSnackbar("🎉 \"$addedTitle\" added to your library!")
                        app.celebrateMilestonesForActiveProfile()
                    }
                }
                LaunchedEffect(updatedTitle) {
                    if (!updatedTitle.isNullOrBlank()) {
                        entry.savedStateHandle.remove<String>("updated_book_title")
                        app.audioManager.playSound(AppSound.BOOK_SAVED)
                        snackbarHostState.showSnackbar("✅ \"$updatedTitle\" saved!")
                    }
                }
                val streakInfo by vm.streakInfo.collectAsStateWithLifecycle()
                LaunchedEffect(sharedKidsState.activeKidId) { vm.refreshActiveKid() }
                LibraryScreen(
                    viewModel = vm,
                    activeReaderLabel = sharedKidsState.activeKid?.let { "${it.emoji} ${it.firstName}" } ?: "Parent",
                    streakInfo = streakInfo,
                    onBookClick = { navController.navigate(Routes.detail(it.id)) },
                    onManualAddClick = { navController.navigate(Routes.ADD) },
                    onScanClick = { navController.navigate(Routes.SCAN) },
                    onMilestonesClick = { navController.navigate(Routes.MILESTONES) },
                )
            }
            composable(Routes.LEADERBOARD) {
                val vm: LeaderboardViewModel = viewModel(factory = LeaderboardViewModel.Factory(app.cloudRepository))
                LeaderboardScreen(
                    viewModel = vm,
                    isSignedIn = authState.user != null,
                    currentUserId = authState.user?.uid,
                    onSignInClick = {
                        navController.navigate(Routes.ACCOUNT) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.ACCOUNT) {
                val kidsVm: KidsViewModel = viewModel(
                    factory = KidsViewModel.Factory(
                        app.kidProfileRepository,
                        app.activeKidPreferences,
                        app.cloudRepository,
                    ),
                )
                ProfileScreen(
                    viewModel = authViewModel,
                    kidsViewModel = kidsVm,
                    audioPreferences = app.audioPreferences,
                    audioManager = app.audioManager,
                    onLeaderboardClick = { navController.navigate(Routes.LEADERBOARD) },
                    onKidProfilesClick = { navController.navigate(Routes.KID_PROFILES) },
                )
            }
            composable(Routes.KID_PROFILES) {
                val kidsVm: KidsViewModel = viewModel(
                    factory = KidsViewModel.Factory(
                        app.kidProfileRepository,
                        app.activeKidPreferences,
                        app.cloudRepository,
                    ),
                )
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
                val kidsVm: KidsViewModel = viewModel(
                    factory = KidsViewModel.Factory(
                        app.kidProfileRepository,
                        app.activeKidPreferences,
                        app.cloudRepository,
                    ),
                )
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
                    activeReaderLabel = sharedKidsState.activeKid?.let { "${it.emoji} ${it.firstName}" } ?: "Parent",
                    onBack = { navController.popBackStack() },
                    onSaved = { _, title ->
                        runCatching {
                            navController.getBackStackEntry(Routes.LIBRARY)
                                .savedStateHandle["added_book_title"] = title
                        }
                        navController.popBackStack()
                    },
                    scannedIsbn = scannedIsbn,
                )
            }
            composable(Routes.SCAN) {
                val scope = rememberCoroutineScope()
                ScanScreen(
                    onBack = { navController.popBackStack() },
                    onIsbnScanned = { isbn ->
                        scope.launch {
                            app.audioManager.playSound(AppSound.SCAN_SUCCESS)
                            app.recordBookScanned()
                            val kidId = app.activeKidPreferences.getActiveKidId()
                            app.repository.lookupByIsbn(isbn)
                                .onSuccess { lookedUp ->
                                    val book = lookedUp.copy(kidProfileId = kidId)
                                    if (book.title.isNotBlank() && book.author.isNotBlank()) {
                                        val id = app.repository.saveBook(book)
                                        app.syncBookToCloud(book.copy(id = id))
                                        runCatching {
                                            navController.getBackStackEntry(Routes.LIBRARY)
                                                .savedStateHandle["added_book_title"] = book.title
                                        }
                                        navController.popBackStack(Routes.LIBRARY, inclusive = false)
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
}