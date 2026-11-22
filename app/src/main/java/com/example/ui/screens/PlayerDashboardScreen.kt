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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
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
import com.example.ui.components.StatTile
import com.example.ui.components.StatusBadge
import com.example.ui.components.UpiQrPaymentCard
import com.example.ui.components.buildMatchShareText
import com.example.ui.components.formatTimestampShort
import com.example.ui.components.hasInternetConnection
import com.example.ui.components.hasNotificationPermission
import com.example.ui.components.notifyRoomCredentialsReady
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
    initialReferralCode: String = "",
    isBusy: Boolean,
    onJoinMatch: (TournamentMatch) -> Unit,
    onSubmitDeposit: (amountText: String, utr: String, onSuccess: () -> Unit) -> Unit,
    onSubmitWithdrawal: (amountText: String, upiId: String, onSuccess: () -> Unit) -> Unit,
    onApplyReferralCode: (code: String, onSuccess: () -> Unit) -> Unit,
    onUpdateProfile: (ign: String, gameUid: String, phone: String, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(PlayerTab.TOURNAMENTS) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var pendingJoinMatch by remember { mutableStateOf<TournamentMatch?>(null) }

    LaunchedEffect(initialReferralCode) {
        if (initialReferralCode.isNotBlank() && profile.referredByCode.isBlank()) {
            selectedTab = PlayerTab.REFER
        }
    }

    BackHandler(enabled = selectedTab != PlayerTab.TOURNAMENTS) {
        selectedTab = PlayerTab.TOURNAMENTS
    }

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
                            onClick = { selectedTab = tab },
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
                        onEditProfile = { showEditProfileDialog = true },
                        onRequestJoin = { pendingJoinMatch = it },
                        onSubmitDeposit = onSubmitDeposit,
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
                                onClick = { selectedTab = tab },
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
                                    selectedIconColor = BlazeOrange,
                                    selectedTextColor = BlazeOrange,
                                    indicatorColor = BlazeOrangeContainer,
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
                    onEditProfile = { showEditProfileDialog = true },
                    onRequestJoin = { pendingJoinMatch = it },
                    onSubmitDeposit = onSubmitDeposit,
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

    if (pendingJoinMatch != null) {
        val match = pendingJoinMatch!!
        val dialogContext = LocalContext.current
        val hasEnoughBalance = profile.wallet >= match.entry
        val shortage = (match.entry - profile.wallet).coerceAtLeast(0L)
        AlertDialog(
            onDismissRequest = { pendingJoinMatch = null },
            containerColor = ArenaSlateDark,
            title = {
                Text(
                    text = if (hasEnoughBalance) "Confirm Match Entry" else "Insufficient Wallet Balance",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (hasEnoughBalance) TextPrimary else BlazeOrange
                )
            },
            text = {
                if (hasEnoughBalance) {
                    val entryText = if (match.entry == 0L) {
                        "Free Entry (₹0) — Tap Join Now to reserve your slot!"
                    } else {
                        "Entry fee ₹${match.entry} will be deducted from your wallet (Current: ₹${profile.wallet})."
                    }
                    Text(
                        text = "Join ${match.title} (${match.game})?\n$entryText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Text(
                        text = "${match.title} entry fee is ₹${match.entry}, and your current wallet balance is ₹${profile.wallet}.\n\nAdd ₹$shortage to your Wallet via UPI QR to join this match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                if (hasEnoughBalance) {
                    Button(
                        onClick = {
                            if (!hasInternetConnection(dialogContext)) {
                                onCopyFeedback("No internet connection. Turn on mobile data or Wi-Fi.")
                            } else {
                                onJoinMatch(match)
                            }
                            pendingJoinMatch = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                        modifier = Modifier.testTag("confirm_join_match_button")
                    ) {
                        Text(if (match.entry == 0L) "Join Free" else "Join (₹${match.entry})")
                    }
                } else {
                    Button(
                        onClick = {
                            pendingJoinMatch = null
                            selectedTab = PlayerTab.WALLET
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldWin,
                            contentColor = ArenaObsidian
                        ),
                        modifier = Modifier.testTag("dialog_go_to_wallet_button")
                    ) {
                        Text(stringResource(R.string.go_to_wallet), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJoinMatch = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
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
    onEditProfile: () -> Unit,
    onRequestJoin: (TournamentMatch) -> Unit,
    onSubmitDeposit: (String, String, () -> Unit) -> Unit,
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
            onEditProfile = onEditProfile,
            onRequestJoin = onRequestJoin,
            onCopyFeedback = onCopyFeedback,
            modifier = modifier
        )

        PlayerTab.WALLET -> WalletSection(
            profile = profile,
            settingsState = settingsState,
            isBusy = isBusy,
            onSubmitDeposit = onSubmitDeposit,
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
    onEditProfile: () -> Unit,
    onRequestJoin: (TournamentMatch) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var feedFilter by rememberSaveable { mutableStateOf("Live") }
    var gameFilter by rememberSaveable { mutableStateOf("All") }
    var modeFilter by rememberSaveable { mutableStateOf("All") }
    var formatFilter by rememberSaveable { mutableStateOf("All") }
    var notificationsGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var alertsDismissed by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        onCopyFeedback(
            if (granted) "Match Room ID notifications enabled!" else "You can enable notifications anytime in Phone Settings."
        )
    }

    val joinedIds = (joinedMatchIdsState as? UiState.Success)?.data ?: emptySet()
    val myRegByMatch = remember(myRegistrationsListState) {
        (myRegistrationsListState as? UiState.Success)?.data?.associateBy { it.matchId } ?: emptyMap()
    }
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()
    val activeFeedState = if (feedFilter == "Live") matchesState else completedMatchesState
    val liveMatchesList = (matchesState as? UiState.Success)?.data ?: emptyList()
    val completedMatchesList = (completedMatchesState as? UiState.Success)?.data ?: emptyList()
    val allMatches = if (feedFilter == "Live") liveMatchesList else completedMatchesList
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

    val gameMatches = remember(allMatches, gameFilter) {
        allMatches.filter { gameFilter == "All" || it.game == gameFilter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PlayerProfileWalletBar(profile = profile, onEditProfile = onEditProfile)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted && !alertsDismissed) {
            item {
                Surface(
                    color = ArenaSlateDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = stringResource(R.string.enable_room_alerts),
                            tint = EmeraldWin,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Get Room ID & Pass Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Allow notifications so you never miss Custom match Room ID.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldWin,
                                contentColor = ArenaObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("enable_notifications_button")
                        ) {
                            Text("Allow", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { alertsDismissed = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Later", color = TextSecondary)
                        }
                    }
                }
            }
        }

        if (settings.sponsorTitle.isNotBlank()) {
            item {
                SponsoredPartnerCard(
                    title = settings.sponsorTitle,
                    subtitle = settings.sponsorSubtitle,
                    targetUrl = settings.sponsorUrl
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    "Live" to "Live Matches (${liveMatchesList.size})",
                    "Results" to "Winners & Results (${completedMatchesList.size})"
                ).forEach { (key, label) ->
                    val selected = feedFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { feedFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldWin,
                            selectedLabelColor = ArenaObsidian,
                            containerColor = ArenaSlateDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = ArenaSlateBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_feed_${key.lowercase()}")
                    )
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("All", "Free Fire MAX", "BGMI")) { option ->
                    FilterChip(
                        selected = gameFilter == option,
                        onClick = { gameFilter = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeOrange,
                            selectedLabelColor = TextPrimary,
                            containerColor = ArenaSlateDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = gameFilter == option,
                            borderColor = ArenaSlateBorder
                        )
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Match Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("All", "Lone Wolf", "Clash Squad", "Custom")) { modeOpt ->
                        val count = if (modeOpt == "All") {
                            gameMatches.size
                        } else {
                            gameMatches.count { MatchMode.fromRaw(it.matchMode).label == modeOpt }
                        }
                        val label = if (allMatches.isNotEmpty()) "$modeOpt ($count)" else modeOpt
                        FilterChip(
                            selected = modeFilter == modeOpt,
                            onClick = { modeFilter = modeOpt },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldWin,
                                selectedLabelColor = ArenaObsidian,
                                containerColor = ArenaSlateDark,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = modeFilter == modeOpt,
                                borderColor = ArenaSlateBorder
                            ),
                            modifier = Modifier.testTag("player_mode_filter_${modeOpt.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Format & Slots",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("All", "1vs1", "2vs2", "4vs4", "Full Map")) { fmtOpt ->
                        val modeScoped = gameMatches.filter {
                            modeFilter == "All" || MatchMode.fromRaw(it.matchMode).label == modeFilter
                        }
                        val count = if (fmtOpt == "All") {
                            modeScoped.size
                        } else {
                            modeScoped.count { TeamFormat.fromRaw(it.teamFormat).label == fmtOpt }
                        }
                        val label = if (allMatches.isNotEmpty()) "$fmtOpt ($count)" else fmtOpt
                        FilterChip(
                            selected = formatFilter == fmtOpt,
                            onClick = { formatFilter = fmtOpt },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlazeOrange,
                                selectedLabelColor = TextPrimary,
                                containerColor = ArenaSlateDark,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = formatFilter == fmtOpt,
                                borderColor = ArenaSlateBorder
                            ),
                            modifier = Modifier.testTag("player_format_filter_${fmtOpt.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        when (activeFeedState) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BlazeOrange)
                }
            }

            is UiState.Error -> item {
                ErrorBannerCard(message = activeFeedState.message)
            }

            is UiState.Success -> {
                val filtered = activeFeedState.data.filter { match ->
                    val mode = MatchMode.fromRaw(match.matchMode).label
                    val format = TeamFormat.fromRaw(match.teamFormat).label
                    (gameFilter == "All" || match.game == gameFilter) &&
                        (modeFilter == "All" || mode == modeFilter) &&
                        (formatFilter == "All" || format == formatFilter)
                }
                if (filtered.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = if (feedFilter == "Live") "No Matches in This Category" else "No Completed Match Results Yet",
                            subtitle = if (feedFilter == "Live") {
                                "Try switching Mode (Lone Wolf / Clash Squad / Custom) or Format (1vs1 / 2vs2 / 4vs4)."
                            } else {
                                "Verified winners and prize payouts appear here once matches finish."
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { match ->
                        TournamentMatchCard(
                            match = match,
                            settings = settings,
                            referralCode = profile.referralCode,
                            alreadyJoined = match.id in joinedIds,
                            myRegistration = myRegByMatch[match.id],
                            isBusy = isBusy,
                            onJoinClick = { onRequestJoin(match) },
                            onCopyFeedback = onCopyFeedback
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SponsoredPartnerCard(
    title: String,
    subtitle: String,
    targetUrl: String
) {
    val context = LocalContext.current
    val hasValidUrl = targetUrl.startsWith("https://") || targetUrl.startsWith("http://")
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sponsored_partner_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = stringResource(R.string.sponsor_badge),
                tint = BlazeOrange,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sponsor_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = BlazeOrange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (hasValidUrl) {
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlazeOrange,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.sponsor_open_button), style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentMatchCard(
    match: TournamentMatch,
    settings: AppSettings,
    referralCode: String,
    alreadyJoined: Boolean,
    myRegistration: MatchRegistration?,
    isBusy: Boolean,
    onJoinClick: () -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isFull = match.joinedCount >= match.slots
    val slotsRemaining = (match.slots - match.joinedCount).coerceAtLeast(0L)
    val slotProgress = if (match.slots > 0L) {
        (match.joinedCount.toFloat() / match.slots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val resolvedMode = MatchMode.fromRaw(match.matchMode).label
    val resolvedFormat = TeamFormat.fromRaw(match.teamFormat).label

    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (alreadyJoined) EmeraldWin.copy(alpha = 0.45f) else ArenaSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Surface(
                                color = BlazeOrangeContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = match.game,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BlazeOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = EmeraldWinContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = resolvedMode,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldWin,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            Surface(
                                color = ArenaSlateSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ArenaSlateBorder)
                            ) {
                                Text(
                                    text = resolvedFormat,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = match.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${match.matchTime} • Map: ${match.mapName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val shareText = buildMatchShareText(match, settings, referralCode)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(sendIntent, "Share Match Invite"))
                            }.onFailure {
                                clipboard.setText(AnnotatedString(shareText))
                                onCopyFeedback("Match invite copied!")
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_match_button_${match.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share_match_invite),
                            tint = BlazeOrange
                        )
                    }
                    Surface(
                        color = ArenaSlateSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isFull) BlazeOrange.copy(alpha = 0.6f) else ArenaSlateBorder)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${match.joinedCount}/${match.slots} Slots",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isFull) BlazeOrange else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFull) "Room Full" else "$slotsRemaining Open",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFull) BlazeOrange else EmeraldWin
                            )
                        }
                    }
                }
            }

            if (match.slots <= 12L) {
                MatchTeamSlotsStrip(
                    totalSlots = match.slots.toInt(),
                    joinedCount = match.joinedCount.toInt(),
                    teamFormat = resolvedFormat
                )
            } else {
                LinearProgressIndicator(
                    progress = { slotProgress },
                    color = if (isFull) BlazeOrange else EmeraldWin,
                    trackColor = ArenaSlateSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("Entry", "₹${match.entry}", TextPrimary, Modifier.weight(1f))
                StatTile("Per Kill", "₹${match.perKill}", BlazeOrange, Modifier.weight(1f))
                StatTile("Prize Pool", "₹${match.prize}", EmeraldWin, Modifier.weight(1f))
            }

            if (alreadyJoined) {
                Surface(
                    color = ArenaObsidian,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = EmeraldWin,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.room_credentials),
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldWin
                            )
                        }
                        CredentialCopyRow(
                            label = "Room ID",
                            value = match.roomId.ifBlank { "Not published yet" },
                            canCopy = match.roomId.isNotBlank(),
                            onCopy = {
                                clipboard.setText(AnnotatedString(match.roomId))
                                onCopyFeedback("Room ID copied!")
                            }
                        )
                        CredentialCopyRow(
                            label = "Password",
                            value = match.roomPass.ifBlank { "Not published yet" },
                            canCopy = match.roomPass.isNotBlank(),
                            onCopy = {
                                clipboard.setText(AnnotatedString(match.roomPass))
                                onCopyFeedback("Room Password copied!")
                            }
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.room_locked_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (match.winnerSummary.isNotBlank() || (myRegistration?.prizeWon ?: 0L) > 0L) {
                Surface(
                    color = EmeraldWinContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (match.winnerSummary.isNotBlank()) {
                            Text(
                                text = "🏆 Match Winners: ${match.winnerSummary}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldWin,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (myRegistration != null && myRegistration.prizeWon > 0L) {
                            Text(
                                text = "You Won ₹${myRegistration.prizeWon} • ${myRegistration.payoutNote.ifBlank { "${myRegistration.kills} Kills" }} (Credited to Wallet)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (MatchStatus.fromRaw(match.status) == MatchStatus.COMPLETED) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = ArenaSlateSurface,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if ((myRegistration?.prizeWon ?: 0L) > 0L) {
                            "Completed • Won ₹${myRegistration?.prizeWon}"
                        } else {
                            "Match Completed"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                when {
                    alreadyJoined -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = EmeraldWinContainer,
                                disabledContentColor = EmeraldWin
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Already Joined", fontWeight = FontWeight.Bold)
                        }
                    }

                    isFull -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = ArenaSlateSurface,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Slots Full", fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Button(
                            onClick = onJoinClick,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlazeOrange,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("join_match_button_${match.id}")
                        ) {
                            Text("Join Match (₹${match.entry})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTeamSlotsStrip(
    totalSlots: Int,
    joinedCount: Int,
    teamFormat: String
) {
    val half = (totalSlots / 2).coerceAtLeast(1)
    Surface(
        color = ArenaObsidian,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Match Slots ($teamFormat)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (slotIndex in 0 until half) {
                        val isTaken = slotIndex < joinedCount
                        Surface(
                            color = if (isTaken) EmeraldWin else ArenaSlateSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isTaken) EmeraldWin else ArenaSlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${slotIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTaken) ArenaObsidian else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelMedium,
                    color = BlazeOrange,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (slotIndex in half until totalSlots) {
                        val isTaken = slotIndex < joinedCount
                        Surface(
                            color = if (isTaken) EmeraldWin else ArenaSlateSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isTaken) EmeraldWin else ArenaSlateBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "#${slotIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTaken) ArenaObsidian else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialCopyRow(
    label: String,
    value: String,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFamily,
                color = if (canCopy) BlazeOrange else TextMuted
            )
        }
        if (canCopy) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletSection(
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onSubmitDeposit: (String, String, () -> Unit) -> Unit,
    onSubmitWithdrawal: (String, String, () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()

    var depAmount by rememberSaveable { mutableStateOf("") }
    var depUtr by rememberSaveable { mutableStateOf("") }
    var wdAmount by rememberSaveable { mutableStateOf("") }
    var wdUpi by rememberSaveable { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BlazeOrange,
        unfocusedBorderColor = ArenaSlateBorder,
        focusedContainerColor = ArenaSlateSurface,
        unfocusedContainerColor = ArenaSlateSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_trophy_banner),
                        contentDescription = "Wallet Trophy Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ArenaObsidian.copy(alpha = 0.92f), ArenaObsidian.copy(alpha = 0.45f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_balance_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "₹${profile.wallet}",
                            style = MaterialTheme.typography.displaySmall,
                            color = EmeraldWin
                        )
                        Text(
                            text = "${profile.ign} • +91 ${profile.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Deposit Card
        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_money_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldWin
                    )

                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ArenaSlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pay to UPI ID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = settings.upiId,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = BlazeOrange
                                )
                                Text(
                                    text = "Receiver: ${settings.upiName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(settings.upiId))
                                    onCopyFeedback("Admin UPI ID copied!")
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Admin UPI ID",
                                    tint = BlazeOrange
                                )
                            }
                        }
                    }

                    UpiQrPaymentCard(
                        upiId = settings.upiId,
                        upiName = settings.upiName,
                        amount = depAmount.toLongOrNull(),
                        onFeedback = onCopyFeedback
                    )

                    Text(
                        text = "Quick Deposit Packs (Auto-Updates QR)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("50", "100", "250", "500").forEach { packAmount ->
                            FilterChip(
                                selected = depAmount == packAmount,
                                onClick = { depAmount = packAmount },
                                label = { Text("₹$packAmount") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldWin,
                                    selectedLabelColor = ArenaObsidian,
                                    containerColor = ArenaSlateSurface,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = depAmount,
                        onValueChange = { depAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_amount_input")
                    )

                    OutlinedTextField(
                        value = depUtr,
                        onValueChange = { depUtr = it.take(32) },
                        label = { Text("12-digit UTR / Ref Number") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_utr_input")
                    )

                    Button(
                        onClick = {
                            onSubmitDeposit(depAmount, depUtr) {
                                depAmount = ""
                                depUtr = ""
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldWin,
                            contentColor = ArenaObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("submit_deposit_button")
                    ) {
                        Text(
                            text = stringResource(R.string.submit_deposit),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Withdrawal Card
        item {
            val enteredWdAmount = wdAmount.toLongOrNull() ?: 0L
            val feePercent = settings.withdrawalFeePercent
            val minWithdrawal = settings.minWithdrawal
            val calcFee = if (enteredWdAmount > 0L) (enteredWdAmount * feePercent) / 100L else 0L
            val calcNet = (enteredWdAmount - calcFee).coerceAtLeast(0L)

            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.withdraw_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = BlazeOrange
                    )
                    Text(
                        text = "Min Withdrawal: ₹$minWithdrawal • Platform Fee: $feePercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = wdAmount,
                        onValueChange = { wdAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Withdraw Amount (Min ₹$minWithdrawal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_amount_input")
                    )

                    if (enteredWdAmount > 0L) {
                        Surface(
                            color = ArenaObsidian,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ArenaSlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Platform Fee ($feePercent%)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("-₹$calcFee", style = MaterialTheme.typography.bodySmall, color = BlazeOrange)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("You Receive via UPI", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                    Text("₹$calcNet", style = MaterialTheme.typography.labelLarge, color = EmeraldWin, fontWeight = FontWeight.ExtraBold)
                                }
                                if (enteredWdAmount < minWithdrawal) {
                                    Text(
                                        text = "Minimum withdrawal is ₹$minWithdrawal.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BlazeOrange
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = wdUpi,
                        onValueChange = { wdUpi = it.take(80) },
                        label = { Text("Your UPI ID (e.g. player@ybl)") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_upi_input")
                    )

                    Button(
                        onClick = {
                            onSubmitWithdrawal(wdAmount, wdUpi) {
                                wdAmount = ""
                                wdUpi = ""
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlazeOrange,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("submit_withdrawal_button")
                    ) {
                        Text(
                            text = stringResource(R.string.request_withdrawal),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
                                        text = "₹${dep.amount} • UTR: ${dep.utr}",
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
        PlayerTab.TOURNAMENTS -> if (selected) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents
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
