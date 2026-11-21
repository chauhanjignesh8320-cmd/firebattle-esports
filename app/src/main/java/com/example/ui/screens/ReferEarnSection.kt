package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.ReferralRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatTile
import com.example.ui.components.formatTimestampShort
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
fun ReferEarnSection(
    profile: UserProfile,
    settingsState: UiState<AppSettings>,
    referralsState: UiState<List<ReferralRecord>>,
    isBusy: Boolean,
    onApplyReferralCode: (code: String, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()
    val bonus = settings.referralBonus
    var friendCodeInput by rememberSaveable { mutableStateOf("") }

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
        // Hero Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(BlazeOrangeContainer, ArenaSlateDark, EmeraldWinContainer)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = BlazeOrange,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.refer_earn_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (bonus <= 0L) {
                                Surface(
                                    color = ArenaSlateSurface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "PAUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (bonus > 0L) {
                                "Invite Friends & Earn ₹$bonus Each"
                            } else {
                                "Referral Program Currently Paused"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = if (bonus > 0L) {
                                "Share your invite code with your Free Fire MAX & BGMI squad. When a friend applies your code, both wallets receive ₹$bonus instantly!"
                            } else {
                                "The tournament organizer has temporarily paused referral rewards. Your past earnings remain safely in your wallet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatTile(
                                label = "Friends Joined",
                                value = profile.referralCount.toString(),
                                valueColor = BlazeOrange,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Total Earned",
                                value = "₹${profile.referralEarnings}",
                                valueColor = EmeraldWin,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Player's Own Referral Code Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CardGiftcard,
                            contentDescription = "Your Referral Code",
                            tint = BlazeOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.refer_your_code_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = profile.referralCode.ifBlank { "GENERATING..." },
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldWin,
                                modifier = Modifier.testTag("my_referral_code_text")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val code = profile.referralCode
                                if (code.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(code))
                                    onCopyFeedback("Referral code $code copied!")
                                }
                            },
                            enabled = profile.referralCode.isNotBlank(),
                            border = BorderStroke(1.dp, BlazeOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlazeOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("copy_referral_code_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.refer_copy_code), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val code = profile.referralCode
                                if (code.isNotBlank()) {
                                    val inviteText = buildString {
                                        append("Join me on Firebattle Esports! ")
                                        append("Play Free Fire MAX & BGMI tournaments and win cash prizes. ")
                                        append("Use my referral code $code")
                                        if (bonus > 0L) {
                                            append(" to get ₹$bonus instant wallet bonus!")
                                        } else {
                                            append(" when signing up!")
                                        }
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, inviteText)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Firebattle Invite"))
                                }
                            },
                            enabled = profile.referralCode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlazeOrange,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("share_referral_invite_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.refer_share_invite), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Apply Friend's Code / Already Claimed Card
        item {
            if (profile.referredByCode.isNotBlank()) {
                Surface(
                    color = EmeraldWinContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Referral Claimed",
                            tint = EmeraldWin,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Referral Claimed (${profile.referredByCode})",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldWin,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your welcome referral bonus has been credited to your wallet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            } else {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.GroupAdd,
                                contentDescription = "Apply Referral Code",
                                tint = EmeraldWin,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.refer_apply_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = if (bonus > 0L) {
                                "Enter your friend's code below to claim ₹$bonus welcome bonus immediately."
                            } else {
                                "Referral rewards are paused right now."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        OutlinedTextField(
                            value = friendCodeInput,
                            onValueChange = { raw ->
                                friendCodeInput = raw.uppercase().filter { it.isLetterOrDigit() }.take(16)
                            },
                            label = { Text("Enter Referral Code (e.g. FB8X2K9P)") },
                            singleLine = true,
                            colors = fieldColors,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("refer_code_input")
                        )

                        Button(
                            onClick = {
                                onApplyReferralCode(friendCodeInput) {
                                    friendCodeInput = ""
                                }
                            },
                            enabled = !isBusy && bonus > 0L && friendCodeInput.length >= 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldWin,
                                contentColor = ArenaObsidian
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("apply_refer_code_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = ArenaObsidian,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.refer_apply_button),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Invited Friends History
        item {
            Text(
                text = stringResource(R.string.refer_friends_joined),
                style = MaterialTheme.typography.titleLarge,
                color = BlazeOrange
            )
        }

        when (referralsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = BlazeOrange) }
            is UiState.Error -> item { ErrorBannerCard(referralsState.message) }
            is UiState.Success -> {
                if (referralsState.data.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = "No Friends Joined Yet",
                            subtitle = "Tap Share Invite above and send your referral code on WhatsApp or Discord!"
                        )
                    }
                } else {
                    items(referralsState.data, key = { it.id }) { rec ->
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
                                        text = rec.referredIgn.ifBlank { "Squad Member" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = formatTimestampShort(rec.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Surface(
                                    color = EmeraldWinContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "+₹${rec.bonusAmount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldWin,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
