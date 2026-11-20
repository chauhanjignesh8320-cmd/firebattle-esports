package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.FirebattleRepository
import com.example.data.firebase.signOut
import com.example.data.model.AppMode
import com.example.data.model.UiState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.FirebattleTopBar
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PlayerDashboardScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.FirebattleTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebattleTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedArenaShell(
                currentUserId = uid,
                userDisplayName = user.displayName ?: "",
                userEmail = user.email ?: ""
            )
        }
    }
}

@Composable
private fun AuthenticatedArenaShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val playerViewModel: PlayerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PlayerViewModel(FirebattleRepository(db), currentUserId)
            }
        }
    )

    val isAdmin by playerViewModel.isAdmin.collectAsStateWithLifecycle()
    val profileState by playerViewModel.profileState.collectAsStateWithLifecycle()
    val settingsState by playerViewModel.settingsState.collectAsStateWithLifecycle()
    val matchesState by playerViewModel.liveMatchesState.collectAsStateWithLifecycle()
    val joinedIdsState by playerViewModel.myRegistrationsState.collectAsStateWithLifecycle()
    val depositsState by playerViewModel.myDepositsState.collectAsStateWithLifecycle()
    val withdrawalsState by playerViewModel.myWithdrawalsState.collectAsStateWithLifecycle()
    val playerBusy by playerViewModel.isBusy.collectAsStateWithLifecycle()

    var currentMode by rememberSaveable { mutableStateOf(AppMode.PLAYER) }

    LaunchedEffect(isAdmin) {
        if (!isAdmin && currentMode == AppMode.ADMIN) {
            currentMode = AppMode.PLAYER
        }
    }

    LaunchedEffect(playerViewModel) {
        playerViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val handleLogout = {
        signOut(
            credentialManager = credentialManager,
            scope = scope
        )
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArenaObsidian),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BlazeOrange)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArenaObsidian),
                contentAlignment = Alignment.Center
            ) {
                ErrorBannerCard(
                    message = pState.message,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        is UiState.Success -> {
            val profile = pState.data
            if (profile == null && !(isAdmin && currentMode == AppMode.ADMIN)) {
                Scaffold(
                    containerColor = ArenaObsidian,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (isAdmin) {
                            FirebattleTopBar(
                                currentMode = currentMode,
                                isAdmin = true,
                                onModeSelected = { mode -> currentMode = mode },
                                onLogoutClick = handleLogout
                            )
                        }
                    }
                ) { setupPadding ->
                    ProfileSetupScreen(
                        existingProfile = null,
                        suggestedName = userDisplayName.take(24),
                        email = userEmail,
                        isBusy = playerBusy,
                        onSaveProfile = { ign, gameUid, phone ->
                            playerViewModel.saveProfile(ign, gameUid, phone, isExisting = false)
                        },
                        onLogout = handleLogout,
                        modifier = Modifier.padding(setupPadding)
                    )
                }
            } else {
                Scaffold(
                    containerColor = ArenaObsidian,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        FirebattleTopBar(
                            currentMode = currentMode,
                            isAdmin = isAdmin,
                            onModeSelected = { mode -> currentMode = mode },
                            onLogoutClick = handleLogout
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isAdmin && currentMode == AppMode.ADMIN) {
                            val adminViewModel: AdminViewModel = viewModel(
                                key = "admin_$currentUserId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY]) {
                                            "APPLICATION_KEY missing from CreationExtras"
                                        }
                                        val databaseId = app.getString(R.string.firestore_database_id)
                                        val db = FirebaseFirestore.getInstance(databaseId)
                                        AdminViewModel(FirebattleRepository(db), currentUserId)
                                    }
                                }
                            )

                            LaunchedEffect(adminViewModel) {
                                adminViewModel.events.collect { msg ->
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }

                            val allMatches by adminViewModel.allMatchesState.collectAsStateWithLifecycle()
                            val regsByMatch by adminViewModel.registrationsByMatchState.collectAsStateWithLifecycle()
                            val allDeposits by adminViewModel.allDepositsState.collectAsStateWithLifecycle()
                            val allWithdrawals by adminViewModel.allWithdrawalsState.collectAsStateWithLifecycle()
                            val allUsers by adminViewModel.allUsersState.collectAsStateWithLifecycle()
                            val adminSettings by adminViewModel.settingsState.collectAsStateWithLifecycle()
                            val adminBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()

                            AdminDashboardScreen(
                                matchesState = allMatches,
                                registrationsState = regsByMatch,
                                depositsState = allDeposits,
                                withdrawalsState = allWithdrawals,
                                usersState = allUsers,
                                settingsState = adminSettings,
                                isBusy = adminBusy,
                                onBackToPlayer = { currentMode = AppMode.PLAYER },
                                onCreateMatch = { title, game, time, mapName, entry, perKill, prize, slots, onSuccess ->
                                    adminViewModel.createMatch(title, game, time, mapName, entry, perKill, prize, slots, onSuccess)
                                },
                                onUpdateRoom = { matchId, rid, rpass ->
                                    adminViewModel.updateRoomCredentials(matchId, rid, rpass)
                                },
                                onToggleMatchStatus = { match ->
                                    adminViewModel.toggleMatchStatus(match)
                                },
                                onDeleteMatch = { matchId ->
                                    adminViewModel.deleteMatch(matchId)
                                },
                                onApproveDeposit = { dep ->
                                    adminViewModel.approveDeposit(dep)
                                },
                                onRejectDeposit = { depId ->
                                    adminViewModel.rejectDeposit(depId)
                                },
                                onMarkWithdrawalPaid = { wdId ->
                                    adminViewModel.markWithdrawalPaid(wdId)
                                },
                                onRejectWithdrawal = { wd ->
                                    adminViewModel.rejectAndRefundWithdrawal(wd)
                                },
                                onAdjustUserWallet = { target, delta, onSuccess ->
                                    adminViewModel.adjustUserWallet(target, delta, onSuccess)
                                },
                                onSaveSettings = { upiId, upiName, adminPhone ->
                                    adminViewModel.saveSettings(upiId, upiName, adminPhone)
                                }
                            )
                        } else if (profile != null) {
                            PlayerDashboardScreen(
                                profile = profile,
                                settingsState = settingsState,
                                matchesState = matchesState,
                                joinedMatchIdsState = joinedIdsState,
                                depositsState = depositsState,
                                withdrawalsState = withdrawalsState,
                                isBusy = playerBusy,
                                onJoinMatch = { match ->
                                    playerViewModel.joinMatch(match, profile)
                                },
                                onSubmitDeposit = { amt, utr, onSuccess ->
                                    playerViewModel.submitDeposit(profile, amt, utr, onSuccess)
                                },
                                onSubmitWithdrawal = { amt, upiId, onSuccess ->
                                    playerViewModel.submitWithdrawal(profile, amt, upiId, onSuccess)
                                },
                                onUpdateProfile = { ign, gameUid, phone, onSuccess ->
                                    playerViewModel.saveProfile(ign, gameUid, phone, isExisting = true, onSuccess = onSuccess)
                                },
                                onCopyFeedback = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
