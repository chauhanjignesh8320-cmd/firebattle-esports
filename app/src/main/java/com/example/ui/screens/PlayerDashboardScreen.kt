package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.GameTitle
import com.example.data.model.MatchMode
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.PlayerTab
import com.example.data.model.ReferralRecord
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TournamentRulesDialog
import com.example.ui.components.TournamentRulesHeroBanner
import com.example.ui.components.formatTimestampShort
import com.example.ui.components.hasInternetConnection
import com.example.ui.components.hasNotificationPermission
import com.example.ui.components.notifyRoomCredentialsReady
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaLightBorder
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletTealDark
import com.example.ui.viewmodel.LudoViewModel

@Composable
fun PlayerDashboardScreen(
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    matchesState: UiState<List<TournamentMatch>>,
    completedMatchesState: UiState<List<TournamentMatch>>,
    joinedMatchIdsState: UiState<Set<String>>,
    myRegistrationsListState: UiState<List<MatchRegistration>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    referralsState: UiState<List<ReferralRecord>>,
    ludoViewModel: LudoViewModel? = null,
    showLudoArena: Boolean = false,
    onSetShowLudoArena: (Boolean) -> Unit = {},
    selectedTab: PlayerTab = PlayerTab.TOURNAMENTS,
    onSelectTab: (PlayerTab) -> Unit = {},
    alertsDialogTrigger: Int = 0,
    initialReferralCode: String = "",
    isBusy: Boolean,
    onJoinMatch: (TournamentMatch, Long, String) -> Unit,
    onSubmitDeposit: (amountText: String, utr: String, onSuccess: () -> Unit) -> Unit,
    onStartPhonePeCheckout: ((Long, (String, String) -> Unit, () -> Unit) -> Unit)? = null,
    onVerifyPhonePeOrder: ((String, (String, String) -> Unit) -> Unit)? = null,
    onSubmitWithdrawal: (amountText: String, upiId: String, onSuccess: () -> Unit) -> Unit,
    onApplyReferralCode: (code: String, onSuccess: () -> Unit) -> Unit,
    onUpdateProfile: (ign: String, gameUid: String, phone: String, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showAlertsDialog by rememberSaveable { mutableStateOf(false) }
    var ludoInitialFormat by rememberSaveable { mutableStateOf("All") }
    var pendingJoinMatch by remember { mutableStateOf<TournamentMatch?>(null) }
    var inspectedMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    LaunchedEffect(alertsDialogTrigger) {
        if (alertsDialogTrigger > 0) {
            showAlertsDialog = true
        }
    }

    LaunchedEffect(initialReferralCode) {
        if (initialReferralCode.isNotBlank() && profile.referredByCode.isBlank()) {
            onSelectTab(PlayerTab.REFER)
        }
    }

    BackHandler(enabled = selectedTab != PlayerTab.TOURNAMENTS && !showLudoArena) {
        onSelectTab(PlayerTab.TOURNAMENTS)
    }

    if (showLudoArena && ludoViewModel != null) {
        val liveAll = (matchesState as? UiState.Success)?.data.orEmpty()
        val completedAll = (completedMatchesState as? UiState.Success)?.data.orEmpty()
        val joinedIds = (joinedMatchIdsState as? UiState.Success)?.data ?: emptySet()
        val myRegs = (myRegistrationsListState as? UiState.Success)?.data.orEmpty()
        LudoArenaScreen(
            ludoViewModel = ludoViewModel,
            profile = profile,
            liveLudoMatches = liveAll.filter { it.game == GameTitle.LUDO.label },
            completedLudoMatches = completedAll.filter { it.game == GameTitle.LUDO.label },
            joinedIds = joinedIds,
            myRegistrations = myRegs,
            initialFormatFilter = ludoInitialFormat,
            isMatchBusy = isBusy,
            onRequestJoinMatch = { pendingJoinMatch = it },
            onInspectMatch = { inspectedMatch = it },
            onBackToHome = { onSetShowLudoArena(false) },
            onOpenWallet = {
                onSetShowLudoArena(false)
                onSelectTab(PlayerTab.WALLET)
            },
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(ArenaObsidian)
        ) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = ArenaSlateDark,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    PlayerTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            icon = { PlayerTabIcon(tab, selected) },
                            label = { Text(playerTabLabel(tab)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = BlazeOrange,
                                selectedTextColor = BlazeOrange,
                                indicatorColor = BlazeOrangeContainer,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PlayerTabContent(
                        selectedTab = selectedTab,
                        profile = profile,
                        settingsState = settingsState,
                        matchesState = matchesState,
                        completedMatchesState = completedMatchesState,
                        joinedMatchIdsState = joinedMatchIdsState,
                        myRegistrationsListState = myRegistrationsListState,
                        depositsState = depositsState,
                        withdrawalsState = withdrawalsState,
                        referralsState = referralsState,
                        initialReferralCode = initialReferralCode,
                        isBusy = isBusy,
                        onSelectTab = onSelectTab,
                        onOpenLudo = { fmt ->
                            ludoInitialFormat = fmt
                            onSetShowLudoArena(true)
                        },
                        onEditProfile = { showEditProfileDialog = true },
                        onRequestJoin = { pendingJoinMatch = it },
                        onInspectMatch = { inspectedMatch = it },
                        onSubmitDeposit = onSubmitDeposit,
                        onStartPhonePeCheckout = onStartPhonePeCheckout,
                        onVerifyPhonePeOrder = onVerifyPhonePeOrder,
                        onSubmitWithdrawal = onSubmitWithdrawal,
                        onApplyReferralCode = onApplyReferralCode,
                        onUpdateProfile = onUpdateProfile,
                        onCopyFeedback = onCopyFeedback,
                        onLogout = onLogout,
                        modifier = Modifier.widthIn(max = 840.dp)
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = ArenaObsidian,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = ArenaSlateDark,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        PlayerTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = { PlayerTabIcon(tab, selected) },
                                label = {
                                    Text(
                                        text = playerTabLabel(tab),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = BlazeOrange.copy(alpha = 0.22f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("player_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                PlayerTabContent(
                    selectedTab = selectedTab,
                    profile = profile,
                    settingsState = settingsState,
                    matchesState = matchesState,
                    completedMatchesState = completedMatchesState,
                    joinedMatchIdsState = joinedMatchIdsState,
                    myRegistrationsListState = myRegistrationsListState,
                    depositsState = depositsState,
                    withdrawalsState = withdrawalsState,
                    referralsState = referralsState,
                    initialReferralCode = initialReferralCode,
                    isBusy = isBusy,
                    onSelectTab = onSelectTab,
                    onOpenLudo = { fmt ->
                        ludoInitialFormat = fmt
                        onSetShowLudoArena(true)
                    },
                    onEditProfile = { showEditProfileDialog = true },
                    onRequestJoin = { pendingJoinMatch = it },
                    onInspectMatch = { inspectedMatch = it },
                    onSubmitDeposit = onSubmitDeposit,
                    onStartPhonePeCheckout = onStartPhonePeCheckout,
                    onVerifyPhonePeOrder = onVerifyPhonePeOrder,
                    onSubmitWithdrawal = onSubmitWithdrawal,
                    onApplyReferralCode = onApplyReferralCode,
                    onUpdateProfile = onUpdateProfile,
                    onCopyFeedback = onCopyFeedback,
                    onLogout = onLogout,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
    }

    if (showAlertsDialog) {
        val liveMatches = (matchesState as? UiState.Success)?.data ?: emptyList()
        val joinedIds = (joinedMatchIdsState as? UiState.Success)?.data ?: emptySet()
        val myRegByMatchAlert = ((myRegistrationsListState as? UiState.Success)?.data ?: emptyList())
            .associateBy { it.matchId }
        val roomReadyMatches = liveMatches.filter { it.id in joinedIds && it.roomId.isNotBlank() }
        val notifGranted = hasNotificationPermission(context)
        val notifLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            onCopyFeedback(
                if (granted) "Match Room ID notifications enabled!" else "You can enable notifications anytime in Phone Settings."
            )
        }
        AlertDialog(
            onDismissRequest = { showAlertsDialog = false },
            containerColor = ArenaSlateDark,
            title = {
                Text("Match & Room Alerts", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (roomReadyMatches.isEmpty()) {
                        Text(
                            text = "You have ${liveMatches.count { it.id in joinedIds }} joined match(es). Room ID & Password appear here 10 minutes before match time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        roomReadyMatches.forEach { m ->
                            val reg = myRegByMatchAlert[m.id]
                            Surface(
                                color = EmeraldWinContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(m.title, style = MaterialTheme.typography.labelLarge, color = EmeraldWin, fontWeight = FontWeight.Bold)
                                    if (reg != null && reg.slotNumber > 0L) {
                                        Text(
                                            text = "Slot #${reg.slotNumber} • Room IGN: ${reg.ign}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BlazeOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text("Room ID: ${m.roomId} • Pass: ${m.roomPass}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaObsidian),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Room Notifications", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAlertsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    val activeJoinMatch = pendingJoinMatch?.let { selected ->
        (matchesState as? UiState.Success)?.data?.firstOrNull { it.id == selected.id } ?: selected
    }
    if (activeJoinMatch != null) {
        val dialogContext = LocalContext.current
        MatchSlotPickerDialog(
            match = activeJoinMatch,
            profile = profile,
            isBusy = isBusy,
            onDismiss = { pendingJoinMatch = null },
            onGoToWallet = {
                pendingJoinMatch = null
                onSelectTab(PlayerTab.WALLET)
            },
            onConfirmSlot = { slotNumber, customIgn ->
                if (!hasInternetConnection(dialogContext)) {
                    onCopyFeedback("No internet connection. Turn on mobile data or Wi-Fi.")
                } else {
                    onJoinMatch(activeJoinMatch, slotNumber, customIgn)
                    pendingJoinMatch = null
                }
            }
        )
    }

    val liveMatchesForInspect = (matchesState as? UiState.Success)?.data.orEmpty()
    val completedMatchesForInspect = (completedMatchesState as? UiState.Success)?.data.orEmpty()
    val allMatchesById = remember(liveMatchesForInspect, completedMatchesForInspect) {
        (liveMatchesForInspect + completedMatchesForInspect).associateBy { it.id }
    }
    val activeInspectedMatch = inspectedMatch?.let { selected ->
        allMatchesById[selected.id] ?: selected
    }
    if (activeInspectedMatch != null) {
        val joinedIds = (joinedMatchIdsState as? UiState.Success)?.data ?: emptySet()
        val myRegs = (myRegistrationsListState as? UiState.Success)?.data.orEmpty()
        MatchRoomDetailsDialog(
            match = activeInspectedMatch,
            alreadyJoined = activeInspectedMatch.id in joinedIds,
            myRegistration = myRegs.firstOrNull { it.matchId == activeInspectedMatch.id },
            allMyRegistrations = myRegs,
            matchesById = allMatchesById,
            onDismiss = { inspectedMatch = null },
            onRequestJoin = {
                val target = activeInspectedMatch
                inspectedMatch = null
                pendingJoinMatch = target
            },
            onCopyFeedback = onCopyFeedback
        )
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            isBusy = isBusy,
            onDismiss = { showEditProfileDialog = false },
            onSave = { ign, uid, phone ->
                onUpdateProfile(ign, uid, phone) {
                    showEditProfileDialog = false
                }
            }
        )
    }
}

@Composable
private fun PlayerTabContent(
    selectedTab: PlayerTab,
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    matchesState: UiState<List<TournamentMatch>>,
    completedMatchesState: UiState<List<TournamentMatch>>,
    joinedMatchIdsState: UiState<Set<String>>,
    myRegistrationsListState: UiState<List<MatchRegistration>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    referralsState: UiState<List<ReferralRecord>>,
    initialReferralCode: String = "",
    isBusy: Boolean,
    onSelectTab: (PlayerTab) -> Unit,
    onOpenLudo: (String) -> Unit,
    onEditProfile: () -> Unit,
    onRequestJoin: (TournamentMatch) -> Unit,
    onInspectMatch: (TournamentMatch) -> Unit,
    onSubmitDeposit: (String, String, () -> Unit) -> Unit,
    onStartPhonePeCheckout: ((Long, (String, String) -> Unit, () -> Unit) -> Unit)? = null,
    onVerifyPhonePeOrder: ((String, (String, String) -> Unit) -> Unit)? = null,
    onSubmitWithdrawal: (String, String, () -> Unit) -> Unit,
    onApplyReferralCode: (String, () -> Unit) -> Unit,
    onUpdateProfile: (String, String, String, () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val joinedCount = (joinedMatchIdsState as? UiState.Success)?.data?.size ?: 0
    when (selectedTab) {
        PlayerTab.TOURNAMENTS -> TournamentsSection(
            profile = profile,
            settingsState = settingsState,
            matchesState = matchesState,
            completedMatchesState = completedMatchesState,
            joinedMatchIdsState = joinedMatchIdsState,
            myRegistrationsListState = myRegistrationsListState,
            isBusy = isBusy,
            onSelectTab = onSelectTab,
            onOpenLudo = onOpenLudo,
            onEditProfile = onEditProfile,
            onRequestJoin = onRequestJoin,
            onInspectMatch = onInspectMatch,
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )

        PlayerTab.WALLET -> PlayerWalletSection(
            profile = profile,
            settingsState = settingsState,
            depositsState = depositsState,
            withdrawalsState = withdrawalsState,
            registrationsState = myRegistrationsListState,
            isBusy = isBusy,
            onBackToHome = { onSelectTab(PlayerTab.TOURNAMENTS) },
            onSubmitDeposit = onSubmitDeposit,
            onStartPhonePeCheckout = onStartPhonePeCheckout,
            onVerifyPhonePeOrder = onVerifyPhonePeOrder,
            onSubmitWithdrawal = onSubmitWithdrawal,
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )

        PlayerTab.REFER -> ReferEarnSection(
            profile = profile,
            settingsState = settingsState,
            referralsState = referralsState,
            initialReferralCode = initialReferralCode,
            isBusy = isBusy,
            onApplyReferralCode = onApplyReferralCode,
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )

        PlayerTab.REQUESTS -> RequestsHistorySection(
            registrationsState = myRegistrationsListState,
            depositsState = depositsState,
            withdrawalsState = withdrawalsState,
            modifier = modifier
        )

        PlayerTab.PROFILE -> ProfileSection(
            profile = profile,
            joinedMatchesCount = joinedCount,
            settingsState = settingsState,
            isBusy = isBusy,
            onUpdateProfile = onUpdateProfile,
            onCopyFeedback = onCopyFeedback,
            onLogout = onLogout,
            modifier = modifier
        )
    }
}

@Composable
private fun PlayerProfileWalletBar(
    profile: UserProfile,
    onEditProfile: () -> Unit
) {
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.ign,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Player Profile",
                            tint = BlazeOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = "+91 ${profile.phone} • UID: ${profile.gameUid}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Surface(
                color = EmeraldWinContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.wallet_balance_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹${profile.wallet}",
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldWin,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentsSection(
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    matchesState: UiState<List<TournamentMatch>>,
    completedMatchesState: UiState<List<TournamentMatch>>,
    joinedMatchIdsState: UiState<Set<String>>,
    myRegistrationsListState: UiState<List<MatchRegistration>>,
    isBusy: Boolean,
    onSelectTab: (PlayerTab) -> Unit,
    onOpenLudo: (String) -> Unit,
    onEditProfile: () -> Unit,
    onRequestJoin: (TournamentMatch) -> Unit,
    onInspectMatch: (TournamentMatch) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showArenaList by rememberSaveable { mutableStateOf(false) }
    var feedFilter by rememberSaveable { mutableStateOf("Upcoming") }
    var gameFilter by rememberSaveable { mutableStateOf("All") }
    var modeFilter by rememberSaveable { mutableStateOf("All") }
    var formatFilter by rememberSaveable { mutableStateOf("All") }
    var onlyJoinedFilter by rememberSaveable { mutableStateOf(false) }
    var activeCategoryTitle by rememberSaveable { mutableStateOf("Tournaments") }
    val notificationsGranted = remember { hasNotificationPermission(context) }

    val joinedIds = (joinedMatchIdsState as? UiState.Success)?.data ?: emptySet()
    val myRegistrations = (myRegistrationsListState as? UiState.Success)?.data ?: emptyList()
    val myRegByMatch = remember(myRegistrations) {
        myRegistrations.associateBy { it.matchId }
    }
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()
    val activeFeedState = if (feedFilter == "Results") completedMatchesState else matchesState
    val liveMatchesList = (matchesState as? UiState.Success)?.data ?: emptyList()
    val completedMatchesList = (completedMatchesState as? UiState.Success)?.data ?: emptyList()
    val previousRoomIds = remember { mutableMapOf<String, String>() }

    LaunchedEffect(liveMatchesList, joinedIds, notificationsGranted) {
        liveMatchesList.forEach { match ->
            val prevRoom = previousRoomIds[match.id]
            if (prevRoom != null && prevRoom.isBlank() && match.roomId.isNotBlank() && match.id in joinedIds) {
                if (notificationsGranted) {
                    notifyRoomCredentialsReady(context, match)
                }
            }
            previousRoomIds[match.id] = match.roomId
        }
    }

    BackHandler(enabled = showArenaList) {
        showArenaList = false
    }

    if (!showArenaList) {
        PlayerHomeArenaSection(
            profile = profile,
            settings = settings,
            liveMatches = liveMatchesList,
            completedMatches = completedMatchesList,
            joinedIds = joinedIds,
            myRegistrations = myRegistrations,
            onOpenArenaFilter = { selection ->
                feedFilter = when {
                    selection.feed == "Results" -> "Results"
                    selection.onlyJoinedOrOngoing || selection.feed == "Ongoing" -> "Ongoing"
                    else -> "Upcoming"
                }
                gameFilter = selection.game
                modeFilter = selection.mode
                formatFilter = selection.format
                onlyJoinedFilter = selection.onlyJoinedOrOngoing
                activeCategoryTitle = selection.title
                showArenaList = true
            },
            onOpenLudo = onOpenLudo,
            onOpenWallet = { onSelectTab(PlayerTab.WALLET) },
            onOpenRefer = { onSelectTab(PlayerTab.REFER) },
            onOpenProfile = { onSelectTab(PlayerTab.PROFILE) },
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian)
    ) {
        // Simple Top Header: Back Arrow + Title only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArenaObsidian)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showArenaList = false },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("back_to_arena_home_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = TextPrimary
                )
            }
            Text(
                text = activeCategoryTitle.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp),
                textAlign = TextAlign.Center
            )
        }

        // Purple 3-Tab Strip: ONGOING | UPCOMING | RESULTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ContestPurple)
        ) {
            listOf("Ongoing", "Upcoming", "Results").forEach { tabKey ->
                val selected = feedFilter == tabKey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            feedFilter = tabKey
                            if (tabKey != "Ongoing") onlyJoinedFilter = false
                        }
                        .testTag("player_feed_${tabKey.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabKey.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 13.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selected) Color.White else Color.Transparent)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeFeedState) {
                is UiState.Loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ContestPurple)
                    }
                }

                is UiState.Error -> item {
                    ErrorBannerCard(message = activeFeedState.message)
                }

                is UiState.Success -> {
                    val categoryFiltered = activeFeedState.data.filter { match ->
                        val isLudoMatch = match.game == GameTitle.LUDO.label
                        val mode = MatchMode.fromRaw(match.matchMode).label
                        val format = TeamFormat.fromRaw(match.teamFormat).label
                        (!isLudoMatch || (feedFilter == "Ongoing" && onlyJoinedFilter)) &&
                            (gameFilter == "All" || match.game == gameFilter) &&
                            (modeFilter == "All" || mode == modeFilter) &&
                            (formatFilter == "All" || format == formatFilter)
                    }
                    val filtered = when (feedFilter) {
                        "Ongoing" -> categoryFiltered.filter { match ->
                            if (onlyJoinedFilter) {
                                match.id in joinedIds
                            } else {
                                match.id in joinedIds || match.roomId.isNotBlank()
                            }
                        }
                        else -> categoryFiltered
                    }

                    if (filtered.isEmpty()) {
                        item {
                            EmptyArenaState(
                                title = when (feedFilter) {
                                    "Ongoing" -> "No Ongoing Matches"
                                    "Results" -> "No Match Results Yet"
                                    else -> "No Matches Available"
                                },
                                subtitle = when (feedFilter) {
                                    "Ongoing" -> "Joined matches or active rooms with Room ID appear here."
                                    "Results" -> "Completed match winners and payouts appear here."
                                    else -> "New tournaments in this category will appear here soon."
                                }
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { match ->
                            TournamentMatchCard(
                                match = match,
                                alreadyJoined = match.id in joinedIds,
                                myRegistration = myRegByMatch[match.id],
                                isBusy = isBusy,
                                onJoinClick = { onRequestJoin(match) },
                                onViewMoreClick = { onInspectMatch(match) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TournamentMatchCard(
    match: TournamentMatch,
    alreadyJoined: Boolean,
    myRegistration: MatchRegistration?,
    isBusy: Boolean,
    onJoinClick: () -> Unit,
    onViewMoreClick: () -> Unit
) {
    val isFull = match.joinedCount >= match.slots
    val slotsRemaining = (match.slots - match.joinedCount).coerceAtLeast(0L)
    val slotProgress = if (match.slots > 0L) {
        (match.joinedCount.toFloat() / match.slots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val resolvedMode = MatchMode.fromRaw(match.matchMode).label
    val resolvedFormat = TeamFormat.fromRaw(match.teamFormat).label
    var showRulesDialog by rememberSaveable(match.id) { mutableStateOf(false) }

    Surface(
        color = ArenaWhiteCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, ContestPurple),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onViewMoreClick)
            .testTag("tournament_match_card_${match.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TournamentRulesHeroBanner(
                match = match,
                onClick = { showRulesDialog = true },
                shape = RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val iconRes = if (match.game == GameTitle.LUDO.label) {
                        R.drawable.img_tile_ludo
                    } else {
                        R.drawable.ic_launcher_art
                    }
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = match.game,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.title.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Time: ${match.matchTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaInkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3-Column Metrics Row 1: PRIZE POOL | PER KILL | ENTRY FEE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MatchCardCoinMetric(
                        label = "PRIZE POOL",
                        value = "${match.prize}",
                        modifier = Modifier.weight(1f)
                    )
                    MatchCardCoinMetric(
                        label = "PER KILL",
                        value = "${match.perKill}",
                        modifier = Modifier.weight(1f)
                    )
                    MatchCardCoinMetric(
                        label = "ENTRY FEE",
                        value = "${match.entry}",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3-Column Metrics Row 2: TYPE | VERSION | MAP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MatchCardTextMetric(
                        label = "TYPE",
                        value = resolvedFormat,
                        modifier = Modifier.weight(1f)
                    )
                    MatchCardTextMetric(
                        label = "VERSION",
                        value = resolvedMode,
                        modifier = Modifier.weight(1f)
                    )
                    MatchCardTextMetric(
                        label = "MAP",
                        value = match.mapName.ifBlank { "Bermuda" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom Row: Progress Bar + JOIN / JOINED Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { slotProgress },
                            color = ContestPurple,
                            trackColor = ArenaLightBorder,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isFull) "Room Full" else "Only $slotsRemaining spots left",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaInkMuted
                            )
                            Text(
                                text = "${match.joinedCount}/${match.slots}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaInkDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    val isCompleted = MatchStatus.fromRaw(match.status) == MatchStatus.COMPLETED
                    val actionLabel = when {
                        isCompleted && (myRegistration?.prizeWon ?: 0L) > 0L -> "WON ₹${myRegistration?.prizeWon}"
                        isCompleted -> "RESULTS"
                        alreadyJoined && myRegistration != null && myRegistration.slotNumber > 0L ->
                            "SLOT #${myRegistration.slotNumber}"
                        alreadyJoined -> "JOINED"
                        isFull -> "FULL"
                        else -> "JOIN"
                    }
                    val actionColor = when {
                        alreadyJoined -> EmeraldWin
                        isCompleted || isFull -> ArenaInkMuted
                        else -> ContestPurple
                    }

                    OutlinedButton(
                        onClick = {
                            if (alreadyJoined || isCompleted || isFull) {
                                onViewMoreClick()
                            } else {
                                onJoinClick()
                            }
                        },
                        enabled = !isBusy,
                        border = BorderStroke(1.2.dp, actionColor),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .widthIn(min = 104.dp)
                            .heightIn(min = 40.dp)
                            .testTag("join_match_button_${match.id}")
                    ) {
                        Text(
                            text = actionLabel,
                            color = actionColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showRulesDialog) {
        TournamentRulesDialog(
            match = match,
            onDismiss = { showRulesDialog = false }
        )
    }
}

@Composable
private fun MatchCardCoinMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = ArenaInkMuted,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                color = CoinGold,
                shape = CircleShape,
                modifier = Modifier.size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "C",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF78350F)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = ArenaInkDark,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MatchCardTextMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = ArenaInkMuted,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = ArenaInkDark,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RequestsHistorySection(
    registrationsState: UiState<List<MatchRegistration>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    modifier: Modifier = Modifier
) {
    val winningEntries = (registrationsState as? UiState.Success)?.data
        ?.filter { it.prizeWon > 0L }
        .orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (winningEntries.isNotEmpty()) {
            item {
                Text(
                    text = "My Match Winnings",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmeraldWin
                )
            }
            items(winningEntries, key = { "win_${it.id}" }) { reg ->
                Surface(
                    color = ArenaSlateDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏆 +₹${reg.prizeWon} • ${reg.payoutNote.ifBlank { "Match Prize" }}",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldWin,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "IGN: ${reg.ign} • ${reg.kills} Kills • ${formatTimestampShort(reg.paidAt ?: reg.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "My Deposit Requests",
                style = MaterialTheme.typography.titleLarge,
                color = EmeraldWin
            )
        }

        when (depositsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = EmeraldWin) }
            is UiState.Error -> item { ErrorBannerCard(depositsState.message) }
            is UiState.Success -> {
                if (depositsState.data.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = "No Deposit Requests",
                            subtitle = "Your UPI deposit submissions and approval statuses will appear here."
                        )
                    }
                } else {
                    items(depositsState.data, key = { "dep_${it.id}" }) { dep ->
                        Surface(
                            color = ArenaSlateDark,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "₹${dep.amount} • Ref: ${dep.utr}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = formatTimestampShort(dep.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                StatusBadge(status = dep.status)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "My Withdrawal Requests",
                style = MaterialTheme.typography.titleLarge,
                color = BlazeOrange
            )
        }

        when (withdrawalsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
            is UiState.Error -> item { ErrorBannerCard(withdrawalsState.message) }
            is UiState.Success -> {
                if (withdrawalsState.data.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = "No Withdrawal Requests",
                            subtitle = "Your payout requests and settlement statuses will appear here."
                        )
                    }
                } else {
                    items(withdrawalsState.data, key = { "wd_${it.id}" }) { wd ->
                        Surface(
                            color = ArenaSlateDark,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (wd.feeAmount > 0L) {
                                            "Net ₹${wd.resolvedNetPayout} (Fee ₹${wd.feeAmount}) • ${wd.upiId}"
                                        } else {
                                            "₹${wd.amount} • UPI: ${wd.upiId}"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = formatTimestampShort(wd.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                StatusBadge(status = wd.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var ign by rememberSaveable { mutableStateOf(profile.ign) }
    var uid by rememberSaveable { mutableStateOf(profile.gameUid) }
    var phone by rememberSaveable { mutableStateOf(profile.phone) }
    var err by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSlateDark,
        title = {
            Text("Edit Player Profile", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ign,
                    onValueChange = { ign = it.take(30); err = null },
                    label = { Text("In-Game Name (IGN)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it.take(30); err = null },
                    label = { Text("Game UID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10); err = null },
                    label = { Text("10-Digit Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                if (err != null) {
                    Text(err!!, style = MaterialTheme.typography.bodySmall, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    when {
                        ign.trim().length < 2 -> err = "IGN must be 2+ chars"
                        uid.trim().length < 3 -> err = "UID must be 3+ chars"
                        phone.trim().length != 10 -> err = "Phone must be 10 digits"
                        else -> onSave(ign.trim(), uid.trim(), phone.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaObsidian)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun PlayerTabIcon(tab: PlayerTab, selected: Boolean) {
    val icon = when (tab) {
        PlayerTab.TOURNAMENTS -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        PlayerTab.WALLET -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
        PlayerTab.REFER -> if (selected) Icons.Filled.CardGiftcard else Icons.Outlined.CardGiftcard
        PlayerTab.REQUESTS -> if (selected) Icons.Filled.History else Icons.Outlined.History
        PlayerTab.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    }
    Icon(imageVector = icon, contentDescription = playerTabLabel(tab))
}

@Composable
private fun playerTabLabel(tab: PlayerTab): String = when (tab) {
    PlayerTab.TOURNAMENTS -> stringResource(R.string.tab_tournaments)
    PlayerTab.WALLET -> stringResource(R.string.tab_wallet)
    PlayerTab.REFER -> stringResource(R.string.tab_refer)
    PlayerTab.REQUESTS -> stringResource(R.string.tab_requests)
    PlayerTab.PROFILE -> stringResource(R.string.tab_profile)
}
