package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import java.util.UUID
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.firebase.LudoRepository
import com.example.data.firebase.signOut
import com.example.data.model.AppMode
import com.example.data.model.PlayerTab
import com.example.data.model.UiState
import com.example.notifications.AdminNotificationHelper
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
import com.example.ui.viewmodel.LudoViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var deepLinkReferralCode by mutableStateOf("")
    private var deepLinkPhonePeOrderId by mutableStateOf("")
    private var openAdminDepositsSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdminNotificationHelper.ensureChannel(applicationContext)
        deepLinkReferralCode = extractReferralCodeFromIntent(intent)
        deepLinkPhonePeOrderId = extractPhonePeOrderIdFromIntent(intent)
        if (intent?.getBooleanExtra(AdminNotificationHelper.EXTRA_OPEN_ADMIN_DEPOSITS, false) == true) {
            openAdminDepositsSignal += 1
        }
        setContent {
            FirebattleTheme {
                AppContent(
                    initialReferralCode = deepLinkReferralCode,
                    phonePeReturnOrderId = deepLinkPhonePeOrderId,
                    openAdminDepositsSignal = openAdminDepositsSignal,
                    onConsumePhonePeReturnOrder = { deepLinkPhonePeOrderId = "" },
                    onConsumeOpenAdminDeposits = { openAdminDepositsSignal = 0 }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val extracted = extractReferralCodeFromIntent(intent)
        if (extracted.isNotBlank()) {
            deepLinkReferralCode = extracted
        }
        val orderId = extractPhonePeOrderIdFromIntent(intent)
        if (orderId.isNotBlank()) {
            deepLinkPhonePeOrderId = orderId
        }
        if (intent.getBooleanExtra(AdminNotificationHelper.EXTRA_OPEN_ADMIN_DEPOSITS, false)) {
            openAdminDepositsSignal += 1
        }
    }

    private fun extractPhonePeOrderIdFromIntent(intent: Intent?): String {
        val uri = intent?.data ?: return ""
        val isCustomPhonePeReturn = uri.scheme.equals("firebattle", ignoreCase = true) &&
            uri.host.equals("phonepe-return", ignoreCase = true)
        val isWebPhonePeReturn = uri.scheme.equals("https", ignoreCase = true) &&
            (uri.host.equals("firebetal.com", ignoreCase = true) ||
                uri.host.equals("www.firebetal.com", ignoreCase = true)) &&
            (uri.path ?: "").startsWith("/phonepe-return")
        if (!isCustomPhonePeReturn && !isWebPhonePeReturn) return ""
        val raw = uri.getQueryParameter("orderId") ?: return ""
        return raw.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(64)
    }

    private fun extractReferralCodeFromIntent(intent: Intent?): String {
        val uri = intent?.data ?: return ""
        val isCustomScheme = uri.scheme.equals("firebattle", ignoreCase = true) &&
            uri.host.equals("refer", ignoreCase = true)
        val isGitHubInviteLink = uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("chauhanjignesh8320-cmd.github.io", ignoreCase = true) &&
            (uri.path ?: "").startsWith("/firebattle-esports")
        val isVercelOrCustomInviteLink = uri.scheme.equals("https", ignoreCase = true) &&
            (uri.host.equals("firebattle-esports-k62s.vercel.app", ignoreCase = true) ||
                uri.host.equals("firebetal.com", ignoreCase = true) ||
                uri.host.equals("www.firebetal.com", ignoreCase = true))
        if (!isCustomScheme && !isGitHubInviteLink && !isVercelOrCustomInviteLink) {
            return ""
        }
        val queryCode = uri.getQueryParameter("code") ?: uri.getQueryParameter("ref")
        val pathCode = (uri.path ?: "")
            .trim('/')
            .split('/')
            .takeIf { it.size >= 2 && it[0].equals("refer", ignoreCase = true) }
            ?.getOrNull(1)
        val raw = queryCode ?: pathCode ?: ""
        return raw.trim().uppercase().filter { it.isLetterOrDigit() }.take(16)
    }
}

@Composable
fun AppContent(
    initialReferralCode: String = "",
    phonePeReturnOrderId: String = "",
    openAdminDepositsSignal: Int = 0,
    onConsumePhonePeReturnOrder: () -> Unit = {},
    onConsumeOpenAdminDeposits: () -> Unit = {}
) {
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
                userEmail = user.email ?: "",
                initialReferralCode = initialReferralCode,
                phonePeReturnOrderId = phonePeReturnOrderId,
                openAdminDepositsSignal = openAdminDepositsSignal,
                onConsumePhonePeReturnOrder = onConsumePhonePeReturnOrder,
                onConsumeOpenAdminDeposits = onConsumeOpenAdminDeposits
            )
        }
    }
}

@Composable
private fun AuthenticatedArenaShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    initialReferralCode: String = "",
    phonePeReturnOrderId: String = "",
    openAdminDepositsSignal: Int = 0,
    onConsumePhonePeReturnOrder: () -> Unit = {},
    onConsumeOpenAdminDeposits: () -> Unit = {}
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

    val ludoViewModel: LudoViewModel = viewModel(
        key = "ludo_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LudoViewModel(LudoRepository(db), currentUserId)
            }
        }
    )

    val isAdmin by playerViewModel.isAdmin.collectAsStateWithLifecycle()
    val profileState by playerViewModel.profileState.collectAsStateWithLifecycle()
    val settingsState by playerViewModel.settingsState.collectAsStateWithLifecycle()
    val matchesState by playerViewModel.liveMatchesState.collectAsStateWithLifecycle()
    val completedMatchesState by playerViewModel.completedMatchesState.collectAsStateWithLifecycle()
    val joinedIdsState by playerViewModel.myRegistrationsState.collectAsStateWithLifecycle()
    val myRegistrationsListState by playerViewModel.myRegistrationsListState.collectAsStateWithLifecycle()
    val depositsState by playerViewModel.myDepositsState.collectAsStateWithLifecycle()
    val withdrawalsState by playerViewModel.myWithdrawalsState.collectAsStateWithLifecycle()
    val referralsState by playerViewModel.myReferralsState.collectAsStateWithLifecycle()
    val playerBusy by playerViewModel.isBusy.collectAsStateWithLifecycle()

    var currentMode by rememberSaveable { mutableStateOf(AppMode.PLAYER) }
    var selectedPlayerTab by rememberSaveable { mutableStateOf(PlayerTab.TOURNAMENTS) }
    var showLudoArena by rememberSaveable { mutableStateOf(false) }
    var alertsDialogTrigger by rememberSaveable { mutableStateOf(0) }
    var adminDepositsTabTrigger by rememberSaveable { mutableIntStateOf(0) }
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AdminNotificationHelper.ensureChannel(context)
        }
    }

    LaunchedEffect(isAdmin) {
        AdminNotificationHelper.setAdminDeviceFlag(context, isAdmin)
        if (isAdmin) {
            AdminNotificationHelper.ensureChannel(context)
            Firebase.messaging.subscribeToTopic(AdminNotificationHelper.TOPIC_ADMIN_DEPOSITS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotificationPermission) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    askedNotificationPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Firebase.messaging.unsubscribeFromTopic(AdminNotificationHelper.TOPIC_ADMIN_DEPOSITS)
            if (currentMode == AppMode.ADMIN) {
                currentMode = AppMode.PLAYER
            }
        }
    }

    LaunchedEffect(isAdmin, currentUserId) {
        if (!isAdmin) return@LaunchedEffect
        var initialSnapshotHandled = false
        val knownPendingIds = mutableSetOf<String>()
        playerViewModel.observeAdminDepositsFlow().collect { allDeposits ->
            val pending = allDeposits.filter { it.status == "Pending" }
            if (!initialSnapshotHandled) {
                initialSnapshotHandled = true
                knownPendingIds.addAll(pending.map { it.id })
                AdminNotificationHelper.markSeenQuietly(context, pending.map { it.id })
            } else {
                val newlyArrived = pending.filter { it.id.isNotBlank() && it.id !in knownPendingIds }
                pending.forEach { if (it.id.isNotBlank()) knownPendingIds.add(it.id) }
                newlyArrived.forEach { dep ->
                    AdminNotificationHelper.showDepositAlert(
                        context = context,
                        depositId = dep.id,
                        ign = dep.ign,
                        phone = dep.phone,
                        amount = dep.amount,
                        utr = dep.utr
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("💰 New Add Cash: ₹${dep.amount} from ${dep.ign}!")
                    }
                }
            }
        }
    }

    LaunchedEffect(isAdmin, openAdminDepositsSignal) {
        if (isAdmin && openAdminDepositsSignal > 0) {
            currentMode = AppMode.ADMIN
            adminDepositsTabTrigger += 1
            onConsumeOpenAdminDeposits()
        }
    }

    LaunchedEffect(playerViewModel) {
        playerViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(ludoViewModel) {
        ludoViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(phonePeReturnOrderId) {
        if (phonePeReturnOrderId.isNotBlank()) {
            val prefs = context.getSharedPreferences("firebattle_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("pending_phonepe_order_id").apply()
            selectedPlayerTab = PlayerTab.WALLET
            currentMode = AppMode.PLAYER
            playerViewModel.verifyPhonePeOrder(phonePeReturnOrderId)
            onConsumePhonePeReturnOrder()
        }
    }

    LaunchedEffect(currentUserId) {
        val prefs = context.getSharedPreferences("firebattle_prefs", Context.MODE_PRIVATE)
        val installId = prefs.getString("install_id", null)?.takeIf { it.isNotBlank() } ?: run {
            val generated = "inst_${UUID.randomUUID().toString().replace("-", "").take(24)}"
            prefs.edit().putString("install_id", generated).apply()
            generated
        }
        val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .distinctSpaceTrimmed()
            .ifBlank { "Android Device" }
        playerViewModel.recordDeviceInstallHeartbeat(
            installId = installId,
            deviceModel = deviceModel,
            appVersion = BuildConfig.VERSION_NAME
        )
    }

    val handleLogout = {
        AdminNotificationHelper.setAdminDeviceFlag(context, false)
        Firebase.messaging.unsubscribeFromTopic(AdminNotificationHelper.TOPIC_ADMIN_DEPOSITS)
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
                        initialReferralCode = initialReferralCode,
                        isBusy = playerBusy,
                        onSaveProfile = { ign, gameUid, phone, referralCode ->
                            playerViewModel.saveProfile(
                                ign = ign,
                                gameUid = gameUid,
                                phone = phone,
                                isExisting = false,
                                optionalFriendReferralCode = referralCode
                            )
                        },
                        onLogout = handleLogout,
                        modifier = Modifier.padding(setupPadding)
                    )
                }
            } else {
                val liveMatchesCount = (matchesState as? UiState.Success)?.data?.size ?: 0
                val joinedCount = (joinedIdsState as? UiState.Success)?.data?.size ?: 0
                val topAlertCount = joinedCount.takeIf { it > 0 } ?: liveMatchesCount

                Scaffold(
                    containerColor = ArenaObsidian,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        val hideTopBar = currentMode == AppMode.PLAYER &&
                            (selectedPlayerTab == PlayerTab.WALLET || (selectedPlayerTab == PlayerTab.TOURNAMENTS && showLudoArena))
                        if (!hideTopBar) {
                            FirebattleTopBar(
                                currentMode = currentMode,
                                isAdmin = isAdmin,
                                onModeSelected = { mode -> currentMode = mode },
                                onLogoutClick = handleLogout,
                                walletBalance = if (currentMode == AppMode.PLAYER) profile?.wallet else null,
                                alertCount = topAlertCount,
                                onWalletClick = { selectedPlayerTab = PlayerTab.WALLET },
                                onAlertsClick = { alertsDialogTrigger += 1 }
                            )
                        }
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
                                        AdminViewModel(FirebattleRepository(db), currentUserId, LudoRepository(db))
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
                            val tournamentBoardsByMatch by adminViewModel.tournamentBoardsByMatchState.collectAsStateWithLifecycle()
                            val allDeposits by adminViewModel.allDepositsState.collectAsStateWithLifecycle()
                            val allWithdrawals by adminViewModel.allWithdrawalsState.collectAsStateWithLifecycle()
                            val ownerPayouts by adminViewModel.ownerPayoutsState.collectAsStateWithLifecycle()
                            val allUsers by adminViewModel.allUsersState.collectAsStateWithLifecycle()
                            val allAppInstalls by adminViewModel.allAppInstallsState.collectAsStateWithLifecycle()
                            val apkDownloadsCount by adminViewModel.apkDownloadsCount.collectAsStateWithLifecycle()
                            val adminSettings by adminViewModel.settingsState.collectAsStateWithLifecycle()
                            val adminBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()

                            AdminDashboardScreen(
                                matchesState = allMatches,
                                registrationsState = regsByMatch,
                                tournamentBoardsByMatch = tournamentBoardsByMatch,
                                depositsState = allDeposits,
                                withdrawalsState = allWithdrawals,
                                ownerPayoutsState = ownerPayouts,
                                usersState = allUsers,
                                appInstallsState = allAppInstalls,
                                apkDownloadsCount = apkDownloadsCount,
                                settingsState = adminSettings,
                                isBusy = adminBusy,
                                openDepositsTrigger = adminDepositsTabTrigger,
                                onBackToPlayer = { currentMode = AppMode.PLAYER },
                                onRefreshApkDownloads = { adminViewModel.refreshApkDownloadCount() },
                                onCreateMatch = { title, game, matchMode, teamFormat, time, mapName, entry, perKill, prize, slots, rules, onSuccess ->
                                    adminViewModel.createMatch(
                                        title = title,
                                        game = game,
                                        matchMode = matchMode,
                                        teamFormat = teamFormat,
                                        matchTime = time,
                                        mapName = mapName,
                                        entryText = entry,
                                        perKillText = perKill,
                                        prizeText = prize,
                                        slotsText = slots,
                                        rules = rules,
                                        onSuccess = onSuccess
                                    )
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
                                onAdjustUserWallet = { target, delta, addAsWinning, onSuccess ->
                                    adminViewModel.adjustUserWallet(target, delta, addAsWinning, onSuccess)
                                },
                                onSaveSettings = { draft ->
                                    adminViewModel.saveSettings(draft)
                                },
                                onWithdrawOwnerProfit = { amountText, upiId, note, availableProfit, onSuccess ->
                                    adminViewModel.withdrawOwnerProfit(
                                        amountText = amountText,
                                        upiId = upiId,
                                        note = note,
                                        availableProfit = availableProfit,
                                        onSuccess = onSuccess
                                    )
                                },
                                onPayMatchReward = { matchId, reg, killsText, amountText, markWinner, note, markCompleted, onSuccess ->
                                    adminViewModel.payMatchReward(
                                        matchId = matchId,
                                        registration = reg,
                                        killsText = killsText,
                                        amountText = amountText,
                                        markWinner = markWinner,
                                        note = note,
                                        markMatchCompleted = markCompleted,
                                        onSuccess = onSuccess
                                    )
                                }
                            )
                        } else if (profile != null) {
                            LaunchedEffect(profile.userId, profile.referralCode) {
                                if (profile.referralCode.isBlank()) {
                                    playerViewModel.ensureReferralCodeIfMissing(profile)
                                }
                            }
                            PlayerDashboardScreen(
                                profile = profile,
                                settingsState = settingsState,
                                matchesState = matchesState,
                                completedMatchesState = completedMatchesState,
                                joinedMatchIdsState = joinedIdsState,
                                myRegistrationsListState = myRegistrationsListState,
                                depositsState = depositsState,
                                withdrawalsState = withdrawalsState,
                                referralsState = referralsState,
                                ludoViewModel = ludoViewModel,
                                showLudoArena = showLudoArena,
                                onSetShowLudoArena = { showLudoArena = it },
                                selectedTab = selectedPlayerTab,
                                onSelectTab = { selectedPlayerTab = it },
                                alertsDialogTrigger = alertsDialogTrigger,
                                initialReferralCode = initialReferralCode,
                                isBusy = playerBusy,
                                onJoinMatch = { match, slotNumber, customIgn ->
                                    playerViewModel.joinMatch(match, profile, slotNumber, customIgn)
                                },
                                onSubmitDeposit = { amt, utr, onSuccess ->
                                    playerViewModel.submitDeposit(profile, amt, utr, onSuccess)
                                },
                                onStartPhonePeCheckout = null,
                                onVerifyPhonePeOrder = null,
                                onSubmitWithdrawal = { amt, upiId, onSuccess ->
                                    playerViewModel.submitWithdrawal(profile, amt, upiId, onSuccess)
                                },
                                onApplyReferralCode = { code, onSuccess ->
                                    playerViewModel.applyReferralCode(profile, code, onSuccess)
                                },
                                onUpdateProfile = { ign, gameUid, phone, onSuccess ->
                                    playerViewModel.saveProfile(ign, gameUid, phone, isExisting = true, onSuccess = onSuccess)
                                },
                                onCopyFeedback = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                                onLogout = handleLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun String.distinctSpaceTrimmed(): String =
    trim().replace(Regex("\\s+"), " ")
