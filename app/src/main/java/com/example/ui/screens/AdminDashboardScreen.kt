package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.R
import com.example.data.model.AdminTab
import com.example.data.model.AppInstall
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.OwnerPayout
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.AdminUpiQrPreviewCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatTile
import com.example.ui.components.StatusBadge
import com.example.ui.components.buildUpiPaymentUri
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.CoinGold
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminSettingsDraft

@Composable
fun AdminDashboardScreen(
    matchesState: UiState<List<TournamentMatch>>,
    registrationsState: UiState<Map<String, List<MatchRegistration>>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    ownerPayoutsState: UiState<List<OwnerPayout>>,
    usersState: UiState<List<UserProfile>>,
    appInstallsState: UiState<List<AppInstall>>,
    apkDownloadsCount: Long?,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onBackToPlayer: () -> Unit,
    onRefreshApkDownloads: () -> Unit,
    onCreateMatch: (String, String, String, String, String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onUpdateRoom: (matchId: String, roomId: String, roomPass: String) -> Unit,
    onToggleMatchStatus: (TournamentMatch) -> Unit,
    onDeleteMatch: (matchId: String) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (String) -> Unit,
    onMarkWithdrawalPaid: (String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest) -> Unit,
    onAdjustUserWallet: (UserProfile, String, Boolean, () -> Unit) -> Unit,
    onSaveSettings: (AdminSettingsDraft) -> Unit,
    onWithdrawOwnerProfit: (amountText: String, upiId: String, note: String, availableProfit: Long, onSuccess: () -> Unit) -> Unit,
    onPayMatchReward: (
        matchId: String,
        registration: MatchRegistration,
        killsText: String,
        amountText: String,
        markWinner: Boolean,
        note: String,
        markMatchCompleted: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.MATCHES) }
    var matchToDelete by remember { mutableStateOf<TournamentMatch?>(null) }
    var userToAdjustWallet by remember { mutableStateOf<UserProfile?>(null) }
    var rewardTarget by remember { mutableStateOf<Pair<TournamentMatch, MatchRegistration>?>(null) }
    var showProfitWithdrawDialog by rememberSaveable { mutableStateOf(false) }

    val profitSummary = remember(depositsState, withdrawalsState, ownerPayoutsState) {
        computeOwnerProfitSummary(depositsState, withdrawalsState, ownerPayoutsState)
    }

    BackHandler {
        if (selectedTab != AdminTab.MATCHES) {
            selectedTab = AdminTab.MATCHES
        } else {
            onBackToPlayer()
        }
    }

    val pendingDepositsCount = (depositsState as? UiState.Success)?.data?.count { it.status == "Pending" } ?: 0
    val pendingWithdrawalsCount = (withdrawalsState as? UiState.Success)?.data?.count { it.status == "Pending" } ?: 0

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
                    AdminTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            icon = { AdminTabIcon(tab, pendingDepositsCount, pendingWithdrawalsCount) },
                            label = { Text(adminTabLabel(tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                    AdminTabBody(
                        selectedTab = selectedTab,
                        matchesState = matchesState,
                        registrationsState = registrationsState,
                        depositsState = depositsState,
                        withdrawalsState = withdrawalsState,
                        ownerPayoutsState = ownerPayoutsState,
                        profitSummary = profitSummary,
                        usersState = usersState,
                        appInstallsState = appInstallsState,
                        apkDownloadsCount = apkDownloadsCount,
                        settingsState = settingsState,
                        isBusy = isBusy,
                        onRefreshApkDownloads = onRefreshApkDownloads,
                        onCreateMatch = onCreateMatch,
                        onUpdateRoom = onUpdateRoom,
                        onToggleMatchStatus = onToggleMatchStatus,
                        onRequestDeleteMatch = { matchToDelete = it },
                        onApproveDeposit = onApproveDeposit,
                        onRejectDeposit = onRejectDeposit,
                        onMarkWithdrawalPaid = onMarkWithdrawalPaid,
                        onRejectWithdrawal = onRejectWithdrawal,
                        onRequestWalletAdjust = { userToAdjustWallet = it },
                        onSaveSettings = onSaveSettings,
                        onOpenProfitWithdraw = { showProfitWithdrawDialog = true },
                        onRequestPayPlayer = { match, reg -> rewardTarget = match to reg },
                        modifier = Modifier.widthIn(max = 860.dp)
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
                        AdminTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                icon = { AdminTabIcon(tab, pendingDepositsCount, pendingWithdrawalsCount) },
                                label = {
                                    Text(
                                        text = adminTabLabel(tab),
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
                                modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                AdminTabBody(
                    selectedTab = selectedTab,
                    matchesState = matchesState,
                    registrationsState = registrationsState,
                    depositsState = depositsState,
                    withdrawalsState = withdrawalsState,
                    ownerPayoutsState = ownerPayoutsState,
                    profitSummary = profitSummary,
                    usersState = usersState,
                    appInstallsState = appInstallsState,
                    apkDownloadsCount = apkDownloadsCount,
                    settingsState = settingsState,
                    isBusy = isBusy,
                    onRefreshApkDownloads = onRefreshApkDownloads,
                    onCreateMatch = onCreateMatch,
                    onUpdateRoom = onUpdateRoom,
                    onToggleMatchStatus = onToggleMatchStatus,
                    onRequestDeleteMatch = { matchToDelete = it },
                    onApproveDeposit = onApproveDeposit,
                    onRejectDeposit = onRejectDeposit,
                    onMarkWithdrawalPaid = onMarkWithdrawalPaid,
                    onRejectWithdrawal = onRejectWithdrawal,
                    onRequestWalletAdjust = { userToAdjustWallet = it },
                    onSaveSettings = onSaveSettings,
                    onOpenProfitWithdraw = { showProfitWithdrawDialog = true },
                    onRequestPayPlayer = { match, reg -> rewardTarget = match to reg },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (matchToDelete != null) {
        val m = matchToDelete!!
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = ArenaSlateDark,
            title = { Text("Delete Tournament?", color = TextPrimary) },
            text = { Text("Delete \"${m.title}\"? This removes the tournament listing permanently.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMatch(m.id)
                        matchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonReject)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (userToAdjustWallet != null) {
        val target = userToAdjustWallet!!
        var deltaText by rememberSaveable(target.userId) { mutableStateOf("") }
        var addAsWinning by rememberSaveable(target.userId) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { userToAdjustWallet = null },
            containerColor = ArenaSlateDark,
            title = { Text("Adjust ${target.ign}'s Wallet", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Total Balance: ₹${target.wallet} (Winning: ₹${target.winningBalance}). Enter positive amount to add (e.g. 50) or negative to deduct (e.g. -50).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it.take(10) },
                        label = { Text("Amount (+/- ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = addAsWinning,
                            onCheckedChange = { addAsWinning = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldWin)
                        )
                        Text(
                            text = "Add to Winning Balance (Withdrawable)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isBusy,
                    onClick = {
                        onAdjustUserWallet(target, deltaText, addAsWinning) {
                            userToAdjustWallet = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange)
                ) {
                    Text("Update Balance")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToAdjustWallet = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
    if (showProfitWithdrawDialog) {
        val defaultUpi = (settingsState as? UiState.Success)?.data?.upiId ?: ""
        val defaultName = (settingsState as? UiState.Success)?.data?.upiName ?: "Firebattle Organizer"
        OwnerProfitWithdrawDialog(
            availableProfit = profitSummary.availableProfit,
            defaultUpiId = defaultUpi,
            defaultUpiName = defaultName,
            isBusy = isBusy,
            onDismiss = { showProfitWithdrawDialog = false },
            onConfirm = { amountText, upiId, note ->
                onWithdrawOwnerProfit(
                    amountText,
                    upiId,
                    note,
                    profitSummary.availableProfit
                ) {
                    showProfitWithdrawDialog = false
                }
            }
        )
    }

    if (rewardTarget != null) {
        val (targetMatch, targetReg) = rewardTarget!!
        PayMatchRewardDialog(
            match = targetMatch,
            registration = targetReg,
            isBusy = isBusy,
            onDismiss = { rewardTarget = null },
            onConfirmPay = { killsText, amountText, markWinner, note, markCompleted ->
                onPayMatchReward(
                    targetMatch.id,
                    targetReg,
                    killsText,
                    amountText,
                    markWinner,
                    note,
                    markCompleted
                ) {
                    rewardTarget = null
                }
            }
        )
    }
}

private data class OwnerProfitSummary(
    val approvedDeposits: Long,
    val paidWithdrawals: Long,
    val ownerWithdrawn: Long,
    val availableProfit: Long
)

private fun computeOwnerProfitSummary(
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    ownerPayoutsState: UiState<List<OwnerPayout>>
): OwnerProfitSummary {
    val approvedDeposits = (depositsState as? UiState.Success)?.data
        ?.filter { it.status == "Approved" }
        ?.sumOf { it.amount } ?: 0L
    val paidWithdrawals = (withdrawalsState as? UiState.Success)?.data
        ?.filter { it.status == "Paid" }
        ?.sumOf { it.resolvedNetPayout } ?: 0L
    val ownerWithdrawn = (ownerPayoutsState as? UiState.Success)?.data
        ?.sumOf { it.amount } ?: 0L
    val available = (approvedDeposits - paidWithdrawals - ownerWithdrawn).coerceAtLeast(0L)
    return OwnerProfitSummary(
        approvedDeposits = approvedDeposits,
        paidWithdrawals = paidWithdrawals,
        ownerWithdrawn = ownerWithdrawn,
        availableProfit = available
    )
}

@Composable
private fun AdminTabBody(
    selectedTab: AdminTab,
    matchesState: UiState<List<TournamentMatch>>,
    registrationsState: UiState<Map<String, List<MatchRegistration>>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    ownerPayoutsState: UiState<List<OwnerPayout>>,
    profitSummary: OwnerProfitSummary,
    usersState: UiState<List<UserProfile>>,
    appInstallsState: UiState<List<AppInstall>>,
    apkDownloadsCount: Long?,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onRefreshApkDownloads: () -> Unit,
    onCreateMatch: (String, String, String, String, String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onUpdateRoom: (String, String, String) -> Unit,
    onToggleMatchStatus: (TournamentMatch) -> Unit,
    onRequestDeleteMatch: (TournamentMatch) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (String) -> Unit,
    onMarkWithdrawalPaid: (String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest) -> Unit,
    onRequestWalletAdjust: (UserProfile) -> Unit,
    onSaveSettings: (AdminSettingsDraft) -> Unit,
    onOpenProfitWithdraw: () -> Unit,
    onRequestPayPlayer: (TournamentMatch, MatchRegistration) -> Unit,
    modifier: Modifier = Modifier
) {
    var adminModeFilter by rememberSaveable { mutableStateOf("All") }
    var adminFormatFilter by rememberSaveable { mutableStateOf("All") }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = BlazeOrange)
                    Column {
                        Text(
                            text = "Owner Control Panel",
                            style = MaterialTheme.typography.titleMedium,
                            color = BlazeOrange
                        )
                        Text(
                            text = "● Live Connected to Cloud Firestore",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldWin
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            AdminTab.MATCHES -> {
                item {
                    AdminEarningsOverviewCard(
                        matchesState = matchesState,
                        withdrawalsState = withdrawalsState,
                        profitSummary = profitSummary,
                        onWithdrawProfitClick = onOpenProfitWithdraw
                    )
                }
                item {
                    CreateTournamentCard(isBusy = isBusy, onCreateMatch = onCreateMatch)
                }
                when (matchesState) {
                    is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
                    is UiState.Error -> item { ErrorBannerCard(matchesState.message) }
                    is UiState.Success -> {
                        val regMap = (registrationsState as? UiState.Success)?.data ?: emptyMap()
                        val filteredAdminMatches = matchesState.data.filter { m ->
                            val mode = com.example.data.model.MatchMode.fromRaw(m.matchMode).label
                            val format = com.example.data.model.TeamFormat.fromRaw(m.teamFormat).label
                            (adminModeFilter == "All" || mode == adminModeFilter) &&
                                (adminFormatFilter == "All" || format == adminFormatFilter)
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(listOf("All", "Lone Wolf", "Clash Squad", "Custom")) { modeOpt ->
                                        androidx.compose.material3.FilterChip(
                                            selected = adminModeFilter == modeOpt,
                                            onClick = { adminModeFilter = modeOpt },
                                            label = { Text(modeOpt) },
                                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldWin,
                                                selectedLabelColor = ArenaObsidian,
                                                containerColor = ArenaSlateDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(listOf("All", "1vs1", "2vs2", "4vs4", "Full Map")) { fmtOpt ->
                                        androidx.compose.material3.FilterChip(
                                            selected = adminFormatFilter == fmtOpt,
                                            onClick = { adminFormatFilter = fmtOpt },
                                            label = { Text(fmtOpt) },
                                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = BlazeOrange,
                                                selectedLabelColor = TextPrimary,
                                                containerColor = ArenaSlateDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (filteredAdminMatches.isEmpty()) {
                            item {
                                EmptyArenaState(
                                    title = "No Matches Published",
                                    subtitle = "Create Lone Wolf, Clash Squad, or Custom rooms above."
                                )
                            }
                        } else {
                            items(filteredAdminMatches, key = { it.id }) { match ->
                                AdminMatchCard(
                                    match = match,
                                    joinedPlayers = regMap[match.id] ?: emptyList(),
                                    isBusy = isBusy,
                                    onUpdateRoom = { rid, rpass -> onUpdateRoom(match.id, rid, rpass) },
                                    onToggleStatus = { onToggleMatchStatus(match) },
                                    onPayPlayerClick = { reg -> onRequestPayPlayer(match, reg) },
                                    onDeleteRequest = { onRequestDeleteMatch(match) }
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.DEPOSITS -> {
                when (depositsState) {
                    is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
                    is UiState.Error -> item { ErrorBannerCard(depositsState.message) }
                    is UiState.Success -> {
                        if (depositsState.data.isEmpty()) {
                            item {
                                EmptyArenaState(
                                    title = "No Deposit Requests",
                                    subtitle = "Player UPI deposit requests will appear here in real time."
                                )
                            }
                        } else {
                            val activeSettings = (settingsState as? UiState.Success)?.data ?: AppSettings()
                            items(depositsState.data, key = { it.id }) { dep ->
                                AdminDepositCard(
                                    deposit = dep,
                                    settings = activeSettings,
                                    isBusy = isBusy,
                                    onApprove = { onApproveDeposit(dep) },
                                    onReject = { onRejectDeposit(dep.id) }
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.WITHDRAWALS -> {
                item {
                    OwnerProfitWithdrawalsSection(
                        profitSummary = profitSummary,
                        ownerPayoutsState = ownerPayoutsState,
                        onWithdrawProfitClick = onOpenProfitWithdraw
                    )
                }
                item {
                    HorizontalDivider(color = ArenaSlateBorder, thickness = 1.dp)
                }
                item {
                    Text(
                        text = "Player Withdrawal Requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Bold
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
                                    subtitle = "Player prize withdrawal requests will appear here."
                                )
                            }
                        } else {
                            items(withdrawalsState.data, key = { it.id }) { wd ->
                                AdminWithdrawalCard(
                                    withdrawal = wd,
                                    isBusy = isBusy,
                                    onMarkPaid = { onMarkWithdrawalPaid(wd.id) },
                                    onRejectRefund = { onRejectWithdrawal(wd) }
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.PLAYERS -> {
                item {
                    AdminAppReachCard(
                        usersState = usersState,
                        appInstallsState = appInstallsState,
                        apkDownloadsCount = apkDownloadsCount,
                        onRefreshApkDownloads = onRefreshApkDownloads
                    )
                }
                when (usersState) {
                    is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
                    is UiState.Error -> item { ErrorBannerCard(usersState.message) }
                    is UiState.Success -> {
                        if (usersState.data.isEmpty()) {
                            item {
                                EmptyArenaState(
                                    title = "No Registered Players",
                                    subtitle = "Players who sign in and complete their profile appear here."
                                )
                            }
                        } else {
                            items(usersState.data, key = { it.userId }) { user ->
                                AdminPlayerCard(
                                    player = user,
                                    onAdjustBalance = { onRequestWalletAdjust(user) }
                                )
                            }
                        }
                    }
                }
            }

            AdminTab.SETTINGS -> {
                item {
                    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()
                    AdminSettingsCard(
                        initialSettings = settings,
                        isBusy = isBusy,
                        onSave = onSaveSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDepositCard(
    deposit: DepositRequest,
    settings: AppSettings,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val previewBonus = if (deposit.status == "Pending") {
        settings.calculateDepositBonus(deposit.amount)
    } else {
        deposit.bonusAmount
    }
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "₹${deposit.amount} • ${deposit.ign}",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldWin
                    )
                    Text(
                        text = "+91 ${deposit.phone} • Ref/UTR: ${deposit.utr}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = TextPrimary
                    )
                    if (previewBonus > 0L) {
                        val bonusLabel = if (deposit.status == "Pending") {
                            "Offer Preview (+${settings.depositBonusPercent}%): +₹$previewBonus Bonus -> Credit ₹${deposit.amount + previewBonus}"
                        } else {
                            "Credited with +₹$previewBonus Offer Bonus (Total ₹${deposit.amount + previewBonus})"
                        }
                        Text(
                            text = bonusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CoinGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formatTimestampShort(deposit.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusBadge(status = deposit.status)
            }

            if (deposit.status == "Pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onApprove,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldWin,
                            contentColor = ArenaObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Approve", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonReject,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWithdrawalCard(
    withdrawal: WithdrawalRequest,
    isBusy: Boolean,
    onMarkPaid: () -> Unit,
    onRejectRefund: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val netPayout = withdrawal.resolvedNetPayout

    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (withdrawal.feeAmount > 0L) {
                            "Pay Net ₹$netPayout • ${withdrawal.ign}"
                        } else {
                            "₹${withdrawal.amount} • ${withdrawal.ign}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = BlazeOrange
                    )
                    Text(
                        text = "UPI: ${withdrawal.upiId} • +91 ${withdrawal.phone}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = EmeraldWin
                    )
                    if (withdrawal.feeAmount > 0L) {
                        Text(
                            text = "Gross: ₹${withdrawal.amount} • Owner Fee Kept: ₹${withdrawal.feeAmount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldWin,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatTimestampShort(withdrawal.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusBadge(status = withdrawal.status)
            }

            if (withdrawal.status == "Pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = buildUpiPaymentUri(
                                upiId = withdrawal.upiId,
                                upiName = withdrawal.ign.ifBlank { "Firebattle Player" },
                                amount = netPayout,
                                transactionNote = "Firebattle Prize Payout"
                            )
                            try {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(uri)),
                                        "Pay ₹$netPayout to ${withdrawal.ign}"
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    "No UPI app found. Copy UPI ID and pay manually.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !isBusy && netPayout > 0L && withdrawal.upiId.contains("@"),
                        border = BorderStroke(1.dp, EmeraldWin),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("admin_pay_upi_button_${withdrawal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = EmeraldWin,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pay ₹$netPayout via UPI",
                            color = EmeraldWin,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(withdrawal.upiId))
                            Toast.makeText(
                                context,
                                "Player UPI copied: ${withdrawal.upiId}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        border = BorderStroke(1.dp, ArenaSlateBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("admin_copy_wd_upi_${withdrawal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy player UPI ID",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onMarkPaid,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldWin,
                            contentColor = ArenaObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Mark Paid", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onRejectRefund,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonReject,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Reject & Refund", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAppReachCard(
    usersState: UiState<List<UserProfile>>,
    appInstallsState: UiState<List<AppInstall>>,
    apkDownloadsCount: Long?,
    onRefreshApkDownloads: () -> Unit
) {
    val usersCount = (usersState as? UiState.Success)?.data?.size ?: 0
    val installs = (appInstallsState as? UiState.Success)?.data.orEmpty()
    val phonesCount = installs.size
    val activeTodayCutoffMs = remember { System.currentTimeMillis() - 24L * 60L * 60L * 1000L }
    val activeTodayCount = remember(installs, activeTodayCutoffMs) {
        installs.count { item ->
            val ts = (item.lastActiveAt ?: item.firstSeenAt)?.toDate()?.time ?: 0L
            ts >= activeTodayCutoffMs
        }
    }

    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_app_reach_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = EmeraldWin,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "App Installs & Active Players",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldWin,
                        fontWeight = FontWeight.Bold
                    )
                }
                androidx.compose.material3.IconButton(
                    onClick = onRefreshApkDownloads,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh APK downloads count",
                        tint = BlazeOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "APK Downloads",
                    value = apkDownloadsCount?.toString() ?: "--",
                    valueColor = CoinGold,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Phones Tracked",
                    value = "$phonesCount",
                    valueColor = EmeraldWin,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "Registered",
                    value = "$usersCount",
                    valueColor = BlazeOrange,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Active (24h)",
                    value = "$activeTodayCount",
                    valueColor = EmeraldWin,
                    modifier = Modifier.weight(1f)
                )
            }

            if (installs.isNotEmpty()) {
                HorizontalDivider(color = ArenaSlateBorder)
                Text(
                    text = "Recent Phones (${installs.size} total)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                installs.take(5).forEach { device ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${device.deviceModel} • v${device.appVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestampShort(device.lastActiveAt ?: device.firstSeenAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPlayerCard(
    player: UserProfile,
    onAdjustBalance: () -> Unit
) {
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(16.dp),
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
                    text = "${player.ign} (UID: ${player.gameUid})",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "+91 ${player.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (player.referralCount > 0L || player.referralCode.isNotBlank()) {
                    Text(
                        text = "Code: ${player.referralCode.ifBlank { "-" }} • Ref: ${player.referralCount} (₹${player.referralEarnings})",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlazeOrange
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${player.wallet}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldWin,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Win: ₹${player.winningBalance}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoinGold
                )
                Button(
                    onClick = onAdjustBalance,
                    colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Text("+/- Balance", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun AdminEarningsOverviewCard(
    matchesState: UiState<List<TournamentMatch>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    profitSummary: OwnerProfitSummary,
    onWithdrawProfitClick: () -> Unit
) {
    val feeRevenue = (withdrawalsState as? UiState.Success)?.data
        ?.filter { it.status != "Rejected" }
        ?.sumOf { it.feeAmount } ?: 0L
    val entryRevenue = (matchesState as? UiState.Success)?.data
        ?.sumOf { it.entry * it.joinedCount } ?: 0L

    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_earnings_overview_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.admin_revenue_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldWin,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Available: ₹${profitSummary.availableProfit}",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldWin,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("UPI In", "₹${profitSummary.approvedDeposits}", EmeraldWin, Modifier.weight(1f))
                StatTile("Players Paid", "₹${profitSummary.paidWithdrawals}", BlazeOrange, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("Owner Withdrawn", "₹${profitSummary.ownerWithdrawn}", TextPrimary, Modifier.weight(1f))
                StatTile("Available Profit", "₹${profitSummary.availableProfit}", EmeraldWin, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("Entry Collected", "₹$entryRevenue", TextPrimary, Modifier.weight(1f))
                StatTile("Withdrawal Fees", "₹$feeRevenue", EmeraldWin, Modifier.weight(1f))
            }
            Button(
                onClick = onWithdrawProfitClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldWin,
                    contentColor = ArenaObsidian
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_withdraw_profit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.admin_withdraw_profit_button)} (₹${profitSummary.availableProfit})",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OwnerProfitWithdrawalsSection(
    profitSummary: OwnerProfitSummary,
    ownerPayoutsState: UiState<List<OwnerPayout>>,
    onWithdrawProfitClick: () -> Unit
) {
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_owner_payouts_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.admin_owner_payouts_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldWin,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Available: ₹${profitSummary.availableProfit} • Withdrawn: ₹${profitSummary.ownerWithdrawn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onWithdrawProfitClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldWin,
                    contentColor = ArenaObsidian
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("admin_tab_withdraw_profit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.admin_withdraw_profit_button),
                    fontWeight = FontWeight.Bold
                )
            }

            when (ownerPayoutsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(color = EmeraldWin, modifier = Modifier.size(24.dp))
                }
                is UiState.Error -> {
                    ErrorBannerCard(ownerPayoutsState.message)
                }
                is UiState.Success -> {
                    if (ownerPayoutsState.data.isEmpty()) {
                        Text(
                            text = "No owner profit withdrawals recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ownerPayoutsState.data.take(10).forEach { payout ->
                                Surface(
                                    color = ArenaSlateSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ArenaSlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "₹${payout.amount} -> ${payout.upiId}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = EmeraldWin,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (payout.note.isNotBlank()) {
                                                Text(
                                                    text = payout.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextPrimary
                                                )
                                            }
                                            Text(
                                                text = formatTimestampShort(payout.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        StatusBadge(status = "Paid")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerProfitWithdrawDialog(
    availableProfit: Long,
    defaultUpiId: String,
    defaultUpiName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amountText: String, upiId: String, note: String) -> Unit
) {
    val context = LocalContext.current
    var amountText by rememberSaveable { mutableStateOf("") }
    var upiId by rememberSaveable(defaultUpiId) { mutableStateOf(defaultUpiId) }
    var note by rememberSaveable { mutableStateOf("") }
    var localFeedback by remember { mutableStateOf("") }

    val parsedAmount = amountText.trim().toLongOrNull()
    val hasValidUpi = upiId.trim().length >= 3 && upiId.trim().contains("@")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSlateDark,
        title = {
            Text(
                text = "Withdraw Organizer Profit",
                color = EmeraldWin,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Profit: ₹$availableProfit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (availableProfit > 0L) {
                        TextButton(
                            onClick = { amountText = availableProfit.toString() },
                            modifier = Modifier.testTag("owner_withdraw_max_button")
                        ) {
                            Text("Max ₹$availableProfit", color = BlazeOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_withdraw_amount_input")
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it.take(80) },
                    label = { Text("Destination UPI ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_withdraw_upi_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(100) },
                    label = { Text("Note (Optional, e.g. Weekly Profit)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_withdraw_note_input")
                )
                if (hasValidUpi && parsedAmount != null && parsedAmount > 0L) {
                    OutlinedButton(
                        onClick = {
                            val uri = buildUpiPaymentUri(
                                upiId = upiId.trim(),
                                upiName = defaultUpiName,
                                amount = parsedAmount
                            )
                            try {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(uri)),
                                        "Transfer via UPI App"
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                                localFeedback = "No UPI app found on this device. Record payout manually below."
                            }
                        },
                        border = BorderStroke(1.dp, BlazeOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("owner_withdraw_open_upi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            tint = BlazeOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open UPI App for ₹$parsedAmount", color = BlazeOrange)
                    }
                }
                if (localFeedback.isNotBlank()) {
                    Text(
                        text = localFeedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = BlazeOrange
                    )
                }
                Text(
                    text = "Records an immutable payout entry in Firestore so Available Profit stays accurate.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = { onConfirm(amountText, upiId, note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldWin,
                    contentColor = ArenaObsidian
                ),
                modifier = Modifier.testTag("owner_withdraw_confirm_button")
            ) {
                Text("Record & Withdraw", fontWeight = FontWeight.Bold)
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
private fun AdminSettingsCard(
    initialSettings: AppSettings,
    isBusy: Boolean,
    onSave: (AdminSettingsDraft) -> Unit
) {
    var upiId by rememberSaveable(initialSettings.upiId) { mutableStateOf(initialSettings.upiId) }
    var upiName by rememberSaveable(initialSettings.upiName) { mutableStateOf(initialSettings.upiName) }
    var adminPhone by rememberSaveable(initialSettings.adminPhone) { mutableStateOf(initialSettings.adminPhone) }
    var referralBonus by rememberSaveable(initialSettings.referralBonus) {
        mutableStateOf(initialSettings.referralBonus.toString())
    }
    var appDownloadUrl by rememberSaveable(initialSettings.appDownloadUrl) {
        mutableStateOf(initialSettings.appDownloadUrl)
    }
    var withdrawalFeePercent by rememberSaveable(initialSettings.withdrawalFeePercent) {
        mutableStateOf(initialSettings.withdrawalFeePercent.toString())
    }
    var minWithdrawal by rememberSaveable(initialSettings.minWithdrawal) {
        mutableStateOf(initialSettings.minWithdrawal.toString())
    }
    var sponsorTitle by rememberSaveable(initialSettings.sponsorTitle) {
        mutableStateOf(initialSettings.sponsorTitle)
    }
    var sponsorSubtitle by rememberSaveable(initialSettings.sponsorSubtitle) {
        mutableStateOf(initialSettings.sponsorSubtitle)
    }
    var sponsorUrl by rememberSaveable(initialSettings.sponsorUrl) {
        mutableStateOf(initialSettings.sponsorUrl)
    }
    var depositBonusPercent by rememberSaveable(initialSettings.depositBonusPercent) {
        mutableStateOf(initialSettings.depositBonusPercent.toString())
    }
    var depositOfferMinAmount by rememberSaveable(initialSettings.depositOfferMinAmount) {
        mutableStateOf(initialSettings.depositOfferMinAmount.toString())
    }
    var depositOfferLabel by rememberSaveable(initialSettings.depositOfferLabel) {
        mutableStateOf(initialSettings.depositOfferLabel)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldWin,
        unfocusedBorderColor = ArenaSlateBorder,
        focusedContainerColor = ArenaSlateSurface,
        unfocusedContainerColor = ArenaSlateSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

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
                text = "UPI, Monetization & Referral Config",
                style = MaterialTheme.typography.titleLarge,
                color = BlazeOrange
            )
            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it.take(80) },
                label = { Text("Your UPI ID") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = upiName,
                onValueChange = { upiName = it.take(60) },
                label = { Text("Receiver Name") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = adminPhone,
                onValueChange = { adminPhone = it.filter { ch -> ch.isDigit() }.take(10) },
                label = { Text("Admin Contact Phone (10-digit)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = withdrawalFeePercent,
                    onValueChange = { withdrawalFeePercent = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Withdraw Fee (0-20%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_withdrawal_fee_input")
                )
                OutlinedTextField(
                    value = minWithdrawal,
                    onValueChange = { minWithdrawal = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Min Withdraw (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_min_withdrawal_input")
                )
            }
            OutlinedTextField(
                value = referralBonus,
                onValueChange = { referralBonus = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Referral Bonus per Invite (₹0 - ₹1000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_referral_bonus_input")
            )
            OutlinedTextField(
                value = appDownloadUrl,
                onValueChange = { appDownloadUrl = it.take(300) },
                label = { Text("App Download / Referral Link (https://...)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_app_download_url_input")
            )

            Text(
                text = "Partner / Sponsor Banner (Monetize Ad Slot)",
                style = MaterialTheme.typography.titleMedium,
                color = EmeraldWin,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedTextField(
                value = sponsorTitle,
                onValueChange = { sponsorTitle = it.take(100) },
                label = { Text("Sponsor Headline (Optional)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sponsor_title_input")
            )
            OutlinedTextField(
                value = sponsorSubtitle,
                onValueChange = { sponsorSubtitle = it.take(180) },
                label = { Text("Sponsor Description (Optional)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sponsor_subtitle_input")
            )
            OutlinedTextField(
                value = sponsorUrl,
                onValueChange = { sponsorUrl = it.take(300) },
                label = { Text("Sponsor Link (https://... Optional)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sponsor_url_input")
            )

            Text(
                text = "Add Money Offer (Deposit Bonus %)",
                style = MaterialTheme.typography.titleMedium,
                color = CoinGold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("0", "5", "10", "15", "20").forEach { pctPreset ->
                    val isSelected = (depositBonusPercent.toLongOrNull() ?: 0L).toString() == pctPreset
                    FilterChip(
                        selected = isSelected,
                        onClick = { depositBonusPercent = pctPreset },
                        label = {
                            Text(
                                text = if (pctPreset == "0") "Off" else "+$pctPreset%",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoinGold,
                            selectedLabelColor = ArenaObsidian,
                            containerColor = ArenaSlateSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ArenaSlateBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = depositBonusPercent,
                    onValueChange = { depositBonusPercent = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Bonus % (0-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_deposit_bonus_percent_input")
                )
                OutlinedTextField(
                    value = depositOfferMinAmount,
                    onValueChange = { depositOfferMinAmount = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Min Deposit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_deposit_offer_min_input")
                )
            }
            OutlinedTextField(
                value = depositOfferLabel,
                onValueChange = { depositOfferLabel = it.take(80) },
                label = { Text("Offer Banner Title (Optional)") },
                placeholder = { Text("e.g. Diwali Offer: +10% Extra Bonus!", color = TextMuted) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_deposit_offer_label_input")
            )
            val previewPct = (depositBonusPercent.toLongOrNull() ?: 0L).coerceIn(0L, 100L)
            val previewMin = (depositOfferMinAmount.toLongOrNull() ?: 0L).coerceAtLeast(0L)
            if (previewPct > 0L) {
                val sampleAmt = maxOf(100L, previewMin)
                val sampleBonus = (sampleAmt * previewPct) / 100L
                Surface(
                    color = EmeraldWin.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offer Preview: Player pays ₹$sampleAmt -> gets ₹$sampleAmt + ₹$sampleBonus Bonus = ₹${sampleAmt + sampleBonus} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldWin,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        AdminSettingsDraft(
                            upiId = upiId,
                            upiName = upiName,
                            adminPhone = adminPhone,
                            referralBonusText = referralBonus,
                            appDownloadUrl = appDownloadUrl,
                            withdrawalFeePercentText = withdrawalFeePercent,
                            minWithdrawalText = minWithdrawal,
                            sponsorTitle = sponsorTitle,
                            sponsorSubtitle = sponsorSubtitle,
                            sponsorUrl = sponsorUrl,
                            depositBonusPercentText = depositBonusPercent,
                            depositOfferMinAmountText = depositOfferMinAmount,
                            depositOfferLabel = depositOfferLabel
                        )
                    )
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldWin,
                    contentColor = ArenaObsidian
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_settings_button")
            ) {
                Text(stringResource(R.string.save_settings), fontWeight = FontWeight.Bold)
            }

            AdminUpiQrPreviewCard(
                upiId = upiId,
                upiName = upiName
            )
        }
    }
}

@Composable
private fun AdminTabIcon(
    tab: AdminTab,
    pendingDeposits: Int,
    pendingWithdrawals: Int
) {
    val icon = when (tab) {
        AdminTab.MATCHES -> Icons.Filled.EmojiEvents
        AdminTab.DEPOSITS -> Icons.Filled.ArrowDownward
        AdminTab.WITHDRAWALS -> Icons.Filled.ArrowUpward
        AdminTab.PLAYERS -> Icons.Filled.Group
        AdminTab.SETTINGS -> Icons.Filled.Settings
    }
    val badgeCount = when (tab) {
        AdminTab.DEPOSITS -> pendingDeposits
        AdminTab.WITHDRAWALS -> pendingWithdrawals
        else -> 0
    }
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = BlazeOrange, contentColor = TextPrimary) {
                    Text(badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = adminTabLabel(tab))
        }
    } else {
        Icon(imageVector = icon, contentDescription = adminTabLabel(tab))
    }
}

@Composable
private fun adminTabLabel(tab: AdminTab): String = when (tab) {
    AdminTab.MATCHES -> stringResource(R.string.admin_tab_matches)
    AdminTab.DEPOSITS -> stringResource(R.string.admin_tab_deposits)
    AdminTab.WITHDRAWALS -> stringResource(R.string.admin_tab_withdrawals)
    AdminTab.PLAYERS -> stringResource(R.string.admin_tab_players)
    AdminTab.SETTINGS -> stringResource(R.string.admin_tab_settings)
}
