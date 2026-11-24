package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
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
import com.example.data.model.MatchRegistration
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.StatusBadge
import com.example.ui.components.UpiQrPaymentCard
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaLightBorder
import com.example.ui.theme.ArenaLightSurface
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletTeal
import com.example.ui.theme.WalletTealDark
import com.google.firebase.Timestamp

private enum class WalletActionPanel {
    NONE,
    ADD_MONEY,
    WITHDRAW,
    TRANSACTIONS
}

private data class WalletTxEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val isCredit: Boolean,
    val status: String?,
    val timestamp: Timestamp?
)

@Composable
fun PlayerWalletSection(
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    registrationsState: UiState<List<MatchRegistration>>,
    isBusy: Boolean,
    onBackToHome: () -> Unit,
    onSubmitDeposit: (String, String, () -> Unit) -> Unit,
    onSubmitWithdrawal: (String, String, () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()
    val deposits = (depositsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val registrations = (registrationsState as? UiState.Success)?.data.orEmpty()

    var activePanel by rememberSaveable { mutableStateOf(WalletActionPanel.NONE) }

    BackHandler {
        if (activePanel != WalletActionPanel.NONE) {
            activePanel = WalletActionPanel.NONE
        } else {
            onBackToHome()
        }
    }

    val balanceBreakdown = remember(profile.wallet, profile.winningBalance, profile.referralEarnings, deposits) {
        val winningBal = profile.winningBalance.coerceIn(0L, profile.wallet)
        val nonWinning = (profile.wallet - winningBal).coerceAtLeast(0L)
        val depositBonusEarned = deposits.filter { it.status == "Approved" }.sumOf { it.bonusAmount }
        val bonusBal = (profile.referralEarnings + depositBonusEarned).coerceIn(0L, nonWinning)
        val depositedBal = (nonWinning - bonusBal).coerceAtLeast(0L)
        Triple(depositedBal, winningBal, bonusBal)
    }
    val (depositedBalance, winningBalance, bonusBalance) = balanceBreakdown

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian)
    ) {
        // Dark Header Bar matching screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (activePanel != WalletActionPanel.NONE) {
                            activePanel = WalletActionPanel.NONE
                        } else {
                            onBackToHome()
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("wallet_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.wallet_my_wallet),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = EmeraldWin.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.45f)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldWin,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Real ₹ UPI",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldWin,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Balance Hero Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 6.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("wallet_total_balance")
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(CoinGold, BlazeOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CoinGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ArenaInkDark
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "₹${profile.wallet}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            // 3 White Breakdown Cards: Deposited | Winning | Bonus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WalletBreakdownCard(
                    amount = depositedBalance,
                    label = stringResource(R.string.wallet_deposited),
                    tag = "wallet_stat_deposited",
                    modifier = Modifier.weight(1f)
                )
                WalletBreakdownCard(
                    amount = winningBalance,
                    label = stringResource(R.string.wallet_winning),
                    tag = "wallet_stat_winning",
                    modifier = Modifier.weight(1f)
                )
                WalletBreakdownCard(
                    amount = bonusBalance,
                    label = stringResource(R.string.wallet_bonus),
                    tag = "wallet_stat_bonus",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // White Rounded-Top Sheet
        Surface(
            color = ArenaWhiteCard,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_my_wallet),
                            style = MaterialTheme.typography.titleLarge,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (activePanel != WalletActionPanel.NONE) {
                            TextButton(
                                onClick = { activePanel = WalletActionPanel.NONE },
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(
                                    text = "Overview",
                                    color = ContestPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3 Action Tiles matching screenshot
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletActionTile(
                            label = stringResource(R.string.wallet_add_money_card),
                            badgeSymbol = "+",
                            isSelected = activePanel == WalletActionPanel.ADD_MONEY,
                            accentColor = EmeraldWin,
                            tag = "wallet_card_add_money",
                            onClick = {
                                activePanel = if (activePanel == WalletActionPanel.ADD_MONEY) {
                                    WalletActionPanel.NONE
                                } else {
                                    WalletActionPanel.ADD_MONEY
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        WalletActionTile(
                            label = stringResource(R.string.wallet_withdraw_card),
                            badgeSymbol = "₹",
                            isSelected = activePanel == WalletActionPanel.WITHDRAW,
                            accentColor = BlazeOrange,
                            tag = "wallet_card_withdraw",
                            onClick = {
                                activePanel = if (activePanel == WalletActionPanel.WITHDRAW) {
                                    WalletActionPanel.NONE
                                } else {
                                    WalletActionPanel.WITHDRAW
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        WalletActionTile(
                            label = stringResource(R.string.wallet_transactions_card),
                            badgeSymbol = "⇄",
                            isSelected = activePanel == WalletActionPanel.TRANSACTIONS,
                            accentColor = ContestPurple,
                            tag = "wallet_card_transactions",
                            onClick = {
                                activePanel = if (activePanel == WalletActionPanel.TRANSACTIONS) {
                                    WalletActionPanel.NONE
                                } else {
                                    WalletActionPanel.TRANSACTIONS
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    AnimatedContent(
                        targetState = activePanel,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "wallet_panel_switch"
                    ) { panel ->
                        when (panel) {
                            WalletActionPanel.NONE -> WalletOverviewPromptCard(
                                profile = profile,
                                settings = settings,
                                pendingDepositsCount = deposits.count { it.status == "Pending" },
                                pendingWithdrawalsCount = withdrawals.count { it.status == "Pending" },
                                onOpenAddMoney = { activePanel = WalletActionPanel.ADD_MONEY },
                                onOpenWithdraw = { activePanel = WalletActionPanel.WITHDRAW },
                                onOpenTransactions = { activePanel = WalletActionPanel.TRANSACTIONS }
                            )

                            WalletActionPanel.ADD_MONEY -> AddMoneyRealUpiPanel(
                                settings = settings,
                                isBusy = isBusy,
                                onSubmitDeposit = onSubmitDeposit,
                                onCopyFeedback = onCopyFeedback
                            )

                            WalletActionPanel.WITHDRAW -> WithdrawRealUpiPanel(
                                profile = profile,
                                winningBalance = winningBalance,
                                settings = settings,
                                isBusy = isBusy,
                                onSubmitWithdrawal = onSubmitWithdrawal
                            )

                            WalletActionPanel.TRANSACTIONS -> WalletTransactionsPanel(
                                depositsState = depositsState,
                                withdrawalsState = withdrawalsState,
                                registrationsState = registrationsState
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletBreakdownCard(
    amount: Long,
    label: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaWhiteCard,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 4.dp,
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(CoinGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaInkDark,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "₹$amount",
                    style = MaterialTheme.typography.titleMedium,
                    color = ContestPurple,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ArenaInkDark,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletActionTile(
    label: String,
    badgeSymbol: String,
    isSelected: Boolean,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaWhiteCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else ArenaLightBorder
        ),
        shadowElevation = if (isSelected) 6.dp else 4.dp,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Vibrant teal/gold wallet icon matching the screenshot style
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(WalletTeal, WalletTealDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(CoinGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeSymbol,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArenaInkDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) accentColor else ArenaInkMuted,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletOverviewPromptCard(
    profile: UserProfile,
    settings: AppSettings,
    pendingDepositsCount: Int,
    pendingWithdrawalsCount: Int,
    onOpenAddMoney: () -> Unit,
    onOpenWithdraw: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    Surface(
        color = ArenaLightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArenaLightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldWin.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CurrencyRupee,
                        contentDescription = null,
                        tint = EmeraldWin,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real Money UPI Wallet (1₹ = ₹1)",
                        style = MaterialTheme.typography.titleSmall,
                        color = ArenaInkDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pay directly via GPay, PhonePe, Paytm or BHIM & withdraw winnings to your UPI ID.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaInkMuted
                    )
                }
            }

            if (settings.hasActiveDepositOffer) {
                val offerHeadline = settings.depositOfferLabel.trim().ifBlank {
                    "ADD MONEY OFFER: +${settings.depositBonusPercent}% EXTRA BONUS!"
                }
                val minHint = if (settings.depositOfferMinAmount > 0L) {
                    "On deposits of ₹${settings.depositOfferMinAmount}+ • Tap Add Money to claim"
                } else {
                    "Extra ${settings.depositBonusPercent}% bonus on every UPI deposit • Tap to claim"
                }
                Surface(
                    color = CoinGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenAddMoney)
                        .testTag("wallet_overview_deposit_offer_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "🎁 $offerHeadline",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = minHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldWin,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (pendingDepositsCount > 0 || pendingWithdrawalsCount > 0) {
                Surface(
                    color = BlazeOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTransactions)
                ) {
                    Text(
                        text = "Pending: $pendingDepositsCount Deposit(s) • $pendingWithdrawalsCount Withdrawal(s) — Tap to view status",
                        style = MaterialTheme.typography.labelMedium,
                        color = BlazeOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenAddMoney,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldWin,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Money", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenWithdraw,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlazeOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Withdraw", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Min Withdraw: ₹${settings.minWithdrawal} • Fee: ${settings.withdrawalFeePercent}% • Player: ${profile.ign}",
                style = MaterialTheme.typography.labelSmall,
                color = ArenaInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddMoneyRealUpiPanel(
    settings: AppSettings,
    isBusy: Boolean,
    onSubmitDeposit: (String, String, () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var depAmount by rememberSaveable { mutableStateOf("100") }
    var depUtr by rememberSaveable { mutableStateOf("") }
    var awaitingUpiConfirmAmount by rememberSaveable { mutableStateOf(0L) }
    var awaitingUpiConfirmRef by rememberSaveable { mutableStateOf("") }
    var lastSubmittedSummary by rememberSaveable { mutableStateOf("") }

    val lightFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldWin,
        unfocusedBorderColor = ArenaLightBorder,
        focusedContainerColor = ArenaWhiteCard,
        unfocusedContainerColor = ArenaWhiteCard,
        focusedTextColor = ArenaInkDark,
        unfocusedTextColor = ArenaInkDark,
        focusedLabelColor = EmeraldWin,
        unfocusedLabelColor = ArenaInkMuted
    )

    Surface(
        color = ArenaLightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArenaLightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.add_money_title),
                style = MaterialTheme.typography.titleMedium,
                color = ArenaInkDark,
                fontWeight = FontWeight.Bold
            )

            if (lastSubmittedSummary.isNotBlank()) {
                Surface(
                    color = EmeraldWin.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_request_sent_banner")
                ) {
                    Text(
                        text = "✅ $lastSubmittedSummary Admin will verify in UPI & credit your wallet shortly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaInkDark,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (settings.hasActiveDepositOffer) {
                val offerHeadline = settings.depositOfferLabel.trim().ifBlank {
                    "ADD MONEY OFFER: +${settings.depositBonusPercent}% EXTRA BONUS!"
                }
                val offerSub = if (settings.depositOfferMinAmount > 0L) {
                    "Valid on deposits of ₹${settings.depositOfferMinAmount} or more. Bonus added automatically on approval."
                } else {
                    "Get +${settings.depositBonusPercent}% extra bonus automatically when Admin approves your deposit."
                }
                Surface(
                    color = CoinGold.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.75f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_money_offer_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "🎁 $offerHeadline",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = offerSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaInkMuted
                        )
                    }
                }
            }

            Surface(
                color = ArenaWhiteCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ArenaLightBorder),
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
                            text = "Pay to Verified UPI ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArenaInkMuted
                        )
                        Text(
                            text = settings.upiId,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFamily,
                            color = BlazeOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Receiver: ${settings.upiName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaInkDark
                        )
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(settings.upiId))
                            onCopyFeedback("Organizer UPI ID copied!")
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

            Text(
                text = "Select Amount (₹)",
                style = MaterialTheme.typography.labelMedium,
                color = ArenaInkDark,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("50", "100", "250", "500").forEach { packAmount ->
                    FilterChip(
                        selected = depAmount == packAmount,
                        onClick = { depAmount = packAmount },
                        label = {
                            Text(
                                text = "₹$packAmount",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldWin,
                            selectedLabelColor = Color.White,
                            containerColor = ArenaWhiteCard,
                            labelColor = ArenaInkDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = depAmount == packAmount,
                            borderColor = ArenaLightBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = depAmount,
                onValueChange = { depAmount = it.filter { ch -> ch.isDigit() } },
                label = { Text("Deposit Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = lightFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_amount_input")
            )

            val enteredDepAmount = depAmount.toLongOrNull() ?: 0L
            if (settings.hasActiveDepositOffer && enteredDepAmount > 0L) {
                val expectedBonus = settings.calculateDepositBonus(enteredDepAmount)
                Surface(
                    color = ArenaWhiteCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (expectedBonus > 0L) EmeraldWin.copy(alpha = 0.55f) else ArenaLightBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_offer_breakdown")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (expectedBonus > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("You Pay via UPI", style = MaterialTheme.typography.bodySmall, color = ArenaInkMuted)
                                Text("₹$enteredDepAmount", style = MaterialTheme.typography.bodySmall, color = ArenaInkDark, fontWeight = FontWeight.SemiBold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Offer Bonus (+${settings.depositBonusPercent}%)", style = MaterialTheme.typography.bodySmall, color = EmeraldWin, fontWeight = FontWeight.SemiBold)
                                Text("+₹$expectedBonus", style = MaterialTheme.typography.bodySmall, color = EmeraldWin, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Wallet Credit", style = MaterialTheme.typography.labelLarge, color = ArenaInkDark)
                                Text("₹${enteredDepAmount + expectedBonus}", style = MaterialTheme.typography.labelLarge, color = ContestPurple, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Text(
                                text = "Add ₹${settings.depositOfferMinAmount}+ to unlock +${settings.depositBonusPercent}% Offer Bonus!",
                                style = MaterialTheme.typography.labelMedium,
                                color = BlazeOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            UpiQrPaymentCard(
                upiId = settings.upiId,
                upiName = settings.upiName,
                amount = depAmount.toLongOrNull(),
                onFeedback = onCopyFeedback,
                isLightSurface = true,
                onUpiRefCaptured = { capturedRef ->
                    depUtr = capturedRef
                },
                onAutoSubmitUpiDeposit = { paidAmount, capturedRef ->
                    awaitingUpiConfirmAmount = 0L
                    awaitingUpiConfirmRef = ""
                    onSubmitDeposit(paidAmount.toString(), capturedRef) {
                        lastSubmittedSummary = "₹$paidAmount request sent to Admin automatically (Ref: $capturedRef)."
                        depUtr = ""
                    }
                },
                onReturnedFromUpiApp = { paidAmount, capturedRef, explicitFailure ->
                    if (!explicitFailure && paidAmount > 0L) {
                        awaitingUpiConfirmAmount = paidAmount
                        awaitingUpiConfirmRef = capturedRef
                    } else {
                        awaitingUpiConfirmAmount = 0L
                        awaitingUpiConfirmRef = ""
                    }
                }
            )

            if (awaitingUpiConfirmAmount > 0L) {
                Surface(
                    color = EmeraldWin.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, EmeraldWin),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upi_return_confirm_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Did ₹$awaitingUpiConfirmAmount get deducted in your UPI app?",
                            style = MaterialTheme.typography.titleSmall,
                            color = ArenaInkDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No need to type UTR — tap below to notify Admin immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArenaInkMuted
                        )
                        Button(
                            onClick = {
                                val amt = awaitingUpiConfirmAmount
                                val ref = depUtr.trim().ifBlank { awaitingUpiConfirmRef }
                                onSubmitDeposit(amt.toString(), ref) {
                                    lastSubmittedSummary = "₹$amt request sent to Admin (Ref: $ref)."
                                    awaitingUpiConfirmAmount = 0L
                                    awaitingUpiConfirmRef = ""
                                    depUtr = ""
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldWin,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("confirm_upi_deducted_button")
                        ) {
                            Text(
                                text = "Yes, ₹$awaitingUpiConfirmAmount Paid — Send to Admin",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = depUtr,
                onValueChange = { depUtr = it.take(32) },
                label = { Text("UTR / UPI Ref Number (Optional)") },
                placeholder = { Text("Auto-filled or leave blank after paying", color = ArenaInkMuted) },
                singleLine = true,
                colors = lightFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deposit_utr_input")
            )

            Button(
                onClick = {
                    val submittedAmount = depAmount.trim()
                    val submittedUtr = depUtr.trim()
                    onSubmitDeposit(submittedAmount, submittedUtr) {
                        lastSubmittedSummary = "₹$submittedAmount deposit request sent to Admin."
                        awaitingUpiConfirmAmount = 0L
                        awaitingUpiConfirmRef = ""
                        depAmount = ""
                        depUtr = ""
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldWin,
                    contentColor = Color.White
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

@Composable
private fun WithdrawRealUpiPanel(
    profile: UserProfile,
    winningBalance: Long,
    settings: AppSettings,
    isBusy: Boolean,
    onSubmitWithdrawal: (String, String, () -> Unit) -> Unit
) {
    var wdAmount by rememberSaveable { mutableStateOf("") }
    var wdUpi by rememberSaveable { mutableStateOf("") }

    val enteredWdAmount = wdAmount.toLongOrNull() ?: 0L
    val feePercent = settings.withdrawalFeePercent
    val minWithdrawal = settings.minWithdrawal
    val calcFee = if (enteredWdAmount > 0L) (enteredWdAmount * feePercent) / 100L else 0L
    val calcNet = (enteredWdAmount - calcFee).coerceAtLeast(0L)

    val lightFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BlazeOrange,
        unfocusedBorderColor = ArenaLightBorder,
        focusedContainerColor = ArenaWhiteCard,
        unfocusedContainerColor = ArenaWhiteCard,
        focusedTextColor = ArenaInkDark,
        unfocusedTextColor = ArenaInkDark,
        focusedLabelColor = BlazeOrange,
        unfocusedLabelColor = ArenaInkMuted
    )

    Surface(
        color = ArenaLightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArenaLightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.withdraw_title),
                style = MaterialTheme.typography.titleMedium,
                color = ArenaInkDark,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = ArenaWhiteCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ArenaLightBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Wallet Balance", style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                        Text(
                            text = "₹${profile.wallet}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ContestPurple,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Match Winnings", style = MaterialTheme.typography.labelSmall, color = ArenaInkMuted)
                        Text(
                            text = "₹$winningBalance",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldWin,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Text(
                text = "Min Withdrawal: ₹$minWithdrawal • Fee: $feePercent% • Only Winning Balance (₹$winningBalance) is withdrawable",
                style = MaterialTheme.typography.labelSmall,
                color = ArenaInkMuted
            )

            if (winningBalance < minWithdrawal) {
                Surface(
                    color = BlazeOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Added/Bonus money cannot be withdrawn directly. Play & win matches to move money into Winning (₹$winningBalance / ₹$minWithdrawal min)!",
                        style = MaterialTheme.typography.bodySmall,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            OutlinedTextField(
                value = wdAmount,
                onValueChange = { wdAmount = it.filter { ch -> ch.isDigit() } },
                label = { Text("Withdraw Amount (Max Winning: ₹$winningBalance)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = lightFieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdraw_amount_input")
            )

            if (enteredWdAmount > 0L) {
                Surface(
                    color = ArenaWhiteCard,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ArenaLightBorder),
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
                            Text("Platform Fee ($feePercent%)", style = MaterialTheme.typography.bodySmall, color = ArenaInkMuted)
                            Text("-₹$calcFee", style = MaterialTheme.typography.bodySmall, color = BlazeOrange, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("You Receive via UPI", style = MaterialTheme.typography.labelLarge, color = ArenaInkDark)
                            Text("₹$calcNet", style = MaterialTheme.typography.labelLarge, color = EmeraldWin, fontWeight = FontWeight.ExtraBold)
                        }
                        if (enteredWdAmount > winningBalance) {
                            Text(
                                text = "Exceeds Winning Balance (₹$winningBalance). Deposited & Bonus money must be played in matches first.",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlazeOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (enteredWdAmount < minWithdrawal) {
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
                colors = lightFieldColors,
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
                    contentColor = Color.White
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

@Composable
private fun WalletTransactionsPanel(
    depositsState: UiState<List<DepositRequest>>,
    withdrawalsState: UiState<List<WithdrawalRequest>>,
    registrationsState: UiState<List<MatchRegistration>>
) {
    val isLoading = depositsState is UiState.Loading ||
        withdrawalsState is UiState.Loading ||
        registrationsState is UiState.Loading

    val deposits = (depositsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val registrations = (registrationsState as? UiState.Success)?.data.orEmpty()

    val entries = remember(deposits, withdrawals, registrations) {
        buildList {
            deposits.forEach { dep ->
                val creditedTotal = dep.amount + dep.bonusAmount
                add(
                    WalletTxEntry(
                        id = "dep_${dep.id}",
                        title = "UPI Deposit • UTR ${dep.utr}",
                        subtitle = if (dep.bonusAmount > 0L) {
                            "Paid ₹${dep.amount} + ₹${dep.bonusAmount} Offer Bonus • ${formatTimestampShort(dep.createdAt)}"
                        } else {
                            formatTimestampShort(dep.createdAt)
                        },
                        amountText = "+₹$creditedTotal",
                        isCredit = true,
                        status = dep.status,
                        timestamp = dep.createdAt
                    )
                )
            }
            withdrawals.forEach { wd ->
                add(
                    WalletTxEntry(
                        id = "wd_${wd.id}",
                        title = "UPI Payout • ${wd.upiId}",
                        subtitle = if (wd.feeAmount > 0L) {
                            "Net ₹${wd.resolvedNetPayout} (Fee ₹${wd.feeAmount}) • ${formatTimestampShort(wd.createdAt)}"
                        } else {
                            formatTimestampShort(wd.createdAt)
                        },
                        amountText = "-₹${wd.amount}",
                        isCredit = false,
                        status = wd.status,
                        timestamp = wd.createdAt
                    )
                )
            }
            registrations.forEach { reg ->
                if (reg.prizeWon > 0L) {
                    add(
                        WalletTxEntry(
                            id = "win_${reg.id}",
                            title = "Match Prize • ${reg.payoutNote.ifBlank { "${reg.kills} Kills" }}",
                            subtitle = formatTimestampShort(reg.paidAt ?: reg.createdAt),
                            amountText = "+₹${reg.prizeWon}",
                            isCredit = true,
                            status = "Paid",
                            timestamp = reg.paidAt ?: reg.createdAt
                        )
                    )
                }
                if (reg.entryPaid > 0L) {
                    add(
                        WalletTxEntry(
                            id = "entry_${reg.id}",
                            title = "Match Entry Fee (${reg.ign})",
                            subtitle = formatTimestampShort(reg.createdAt),
                            amountText = "-₹${reg.entryPaid}",
                            isCredit = false,
                            status = "Joined",
                            timestamp = reg.createdAt
                        )
                    )
                }
            }
        }.sortedByDescending { it.timestamp?.seconds ?: 0L }
    }

    Surface(
        color = ArenaLightSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ArenaLightBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Recent Wallet Transactions",
                style = MaterialTheme.typography.titleMedium,
                color = ArenaInkDark,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ContestPurple)
                }
            } else if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = ArenaInkMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Transactions Yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = ArenaInkDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Your UPI deposits, withdrawals, match entries, and winnings will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ArenaInkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                entries.forEach { item ->
                    val icon: ImageVector = when {
                        item.id.startsWith("win_") -> Icons.Filled.CheckCircle
                        item.id.startsWith("entry_") -> Icons.Filled.SportsEsports
                        item.isCredit -> Icons.Filled.ArrowDownward
                        else -> Icons.Filled.ArrowUpward
                    }
                    Surface(
                        color = ArenaWhiteCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ArenaLightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isCredit) EmeraldWin.copy(alpha = 0.14f)
                                        else BlazeOrange.copy(alpha = 0.14f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (item.isCredit) EmeraldWin else BlazeOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ArenaInkDark,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ArenaInkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.amountText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (item.isCredit) EmeraldWin else BlazeOrange,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (!item.status.isNullOrBlank()) {
                                    StatusBadge(status = item.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
