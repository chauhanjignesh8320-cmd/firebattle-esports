package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdminTab
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.AdminUpiQrPreviewCard
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminDashboardScreen(
    matchesState: UiState<List<TournamentMatch>>,
    registrationsState: UiState<Map<String, List<MatchRegistration>>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    usersState: UiState<List<UserProfile>>,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onBackToPlayer: () -> Unit,
    onCreateMatch: (String, String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onUpdateRoom: (matchId: String, roomId: String, roomPass: String) -> Unit,
    onToggleMatchStatus: (TournamentMatch) -> Unit,
    onDeleteMatch: (matchId: String) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (String) -> Unit,
    onMarkWithdrawalPaid: (String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest) -> Unit,
    onAdjustUserWallet: (UserProfile, String, () -> Unit) -> Unit,
    onSaveSettings: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AdminTab.MATCHES) }
    var matchToDelete by remember { mutableStateOf<TournamentMatch?>(null) }
    var userToAdjustWallet by remember { mutableStateOf<UserProfile?>(null) }

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
                        usersState = usersState,
                        settingsState = settingsState,
                        isBusy = isBusy,
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
                    usersState = usersState,
                    settingsState = settingsState,
                    isBusy = isBusy,
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
        AlertDialog(
            onDismissRequest = { userToAdjustWallet = null },
            containerColor = ArenaSlateDark,
            title = { Text("Adjust ${target.ign}'s Wallet", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current Balance: ₹${target.wallet}. Enter positive amount to add (e.g. 50) or negative to deduct (e.g. -50).",
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
                }
            },
            confirmButton = {
                Button(
                    enabled = !isBusy,
                    onClick = {
                        onAdjustUserWallet(target, deltaText) {
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
}

@Composable
private fun AdminTabBody(
    selectedTab: AdminTab,
    matchesState: UiState<List<TournamentMatch>>,
    registrationsState: UiState<Map<String, List<MatchRegistration>>>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    usersState: UiState<List<UserProfile>>,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onCreateMatch: (String, String, String, String, String, String, String, String, () -> Unit) -> Unit,
    onUpdateRoom: (String, String, String) -> Unit,
    onToggleMatchStatus: (TournamentMatch) -> Unit,
    onRequestDeleteMatch: (TournamentMatch) -> Unit,
    onApproveDeposit: (DepositRequest) -> Unit,
    onRejectDeposit: (String) -> Unit,
    onMarkWithdrawalPaid: (String) -> Unit,
    onRejectWithdrawal: (WithdrawalRequest) -> Unit,
    onRequestWalletAdjust: (UserProfile) -> Unit,
    onSaveSettings: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    CreateTournamentCard(isBusy = isBusy, onCreateMatch = onCreateMatch)
                }
                when (matchesState) {
                    is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
                    is UiState.Error -> item { ErrorBannerCard(matchesState.message) }
                    is UiState.Success -> {
                        val regMap = (registrationsState as? UiState.Success)?.data ?: emptyMap()
                        if (matchesState.data.isEmpty()) {
                            item {
                                EmptyArenaState(
                                    title = "No Matches Published",
                                    subtitle = "Create your first Free Fire MAX or BGMI tournament above."
                                )
                            }
                        } else {
                            items(matchesState.data, key = { it.id }) { match ->
                                AdminMatchCard(
                                    match = match,
                                    joinedPlayers = regMap[match.id] ?: emptyList(),
                                    isBusy = isBusy,
                                    onUpdateRoom = { rid, rpass -> onUpdateRoom(match.id, rid, rpass) },
                                    onToggleStatus = { onToggleMatchStatus(match) },
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
                            items(depositsState.data, key = { it.id }) { dep ->
                                AdminDepositCard(
                                    deposit = dep,
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
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
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
                        text = "+91 ${deposit.phone} • UTR: ${deposit.utr}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = TextPrimary
                    )
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
                        text = "₹${withdrawal.amount} • ${withdrawal.ign}",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlazeOrange
                    )
                    Text(
                        text = "UPI: ${withdrawal.upiId} • +91 ${withdrawal.phone}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = EmeraldWin
                    )
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
private fun AdminSettingsCard(
    initialSettings: AppSettings,
    isBusy: Boolean,
    onSave: (upiId: String, upiName: String, adminPhone: String, referralBonusText: String) -> Unit
) {
    var upiId by rememberSaveable(initialSettings.upiId) { mutableStateOf(initialSettings.upiId) }
    var upiName by rememberSaveable(initialSettings.upiName) { mutableStateOf(initialSettings.upiName) }
    var adminPhone by rememberSaveable(initialSettings.adminPhone) { mutableStateOf(initialSettings.adminPhone) }
    var referralBonus by rememberSaveable(initialSettings.referralBonus) {
        mutableStateOf(initialSettings.referralBonus.toString())
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
                text = "UPI, Support & Referral Configuration",
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
            Button(
                onClick = { onSave(upiId, upiName, adminPhone, referralBonus) },
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
