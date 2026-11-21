package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatTile
import com.example.ui.components.formatTimestampShort
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileSection(
    profile: UserProfile,
    joinedMatchesCount: Int,
    settingsState: UiState<AppSettings>,
    isBusy: Boolean,
    onUpdateProfile: (ign: String, gameUid: String, phone: String, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val settings = (settingsState as? UiState.Success)?.data ?: AppSettings()

    var ign by rememberSaveable(profile.ign) { mutableStateOf(profile.ign) }
    var gameUid by rememberSaveable(profile.gameUid) { mutableStateOf(profile.gameUid) }
    var phone by rememberSaveable(profile.phone) { mutableStateOf(profile.phone) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val initials = profile.ign.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }
        .ifBlank { "FB" }

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
        // Player Profile Header & Career Stats
        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_header_card")
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
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(BlazeOrange, EmeraldWin))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ArenaObsidian,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.ign.ifBlank { "Firebattle Player" },
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = "Verified Player",
                                        tint = EmeraldWin,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (profile.email.isNotBlank()) {
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Joined ${formatTimestampShort(profile.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatTile(
                                label = "Matches",
                                value = joinedMatchesCount.toString(),
                                valueColor = BlazeOrange,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Wallet",
                                value = "₹${profile.wallet}",
                                valueColor = EmeraldWin,
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                label = "Referrals",
                                value = profile.referralCount.toString(),
                                valueColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick IDs & Referral Card
        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_ids_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = BlazeOrange
                    )

                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ArenaSlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Game UID", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = profile.gameUid.ifBlank { "-" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (profile.gameUid.isNotBlank()) {
                                        clipboard.setText(AnnotatedString(profile.gameUid))
                                        onCopyFeedback("Game UID copied!")
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("profile_copy_uid_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Game UID",
                                    tint = BlazeOrange
                                )
                            }
                        }
                    }

                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CardGiftcard,
                                    contentDescription = null,
                                    tint = EmeraldWin,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("My Referral Code", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = profile.referralCode.ifBlank { "GENERATING..." },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = JetBrainsMonoFamily,
                                        color = EmeraldWin,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (profile.referralCode.isNotBlank()) {
                                        clipboard.setText(AnnotatedString(profile.referralCode))
                                        onCopyFeedback("Referral code ${profile.referralCode} copied!")
                                    }
                                },
                                enabled = profile.referralCode.isNotBlank(),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("profile_copy_referral_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Referral Code",
                                    tint = EmeraldWin
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Profile Form Card
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
                        text = stringResource(R.string.profile_edit_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldWin
                    )

                    OutlinedTextField(
                        value = ign,
                        onValueChange = { ign = it.take(30); localError = null },
                        label = { Text("In-Game Name (IGN)") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = BlazeOrange) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_tab_ign_input")
                    )

                    OutlinedTextField(
                        value = gameUid,
                        onValueChange = { gameUid = it.take(30); localError = null },
                        label = { Text("Game UID (Free Fire MAX / BGMI)") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = BlazeOrange) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_tab_uid_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() }.take(10)
                            localError = null
                        },
                        label = { Text("10-Digit Mobile Number") },
                        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = BlazeOrange) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_tab_phone_input")
                    )

                    if (localError != null) {
                        ErrorBannerCard(message = localError!!)
                    }

                    Button(
                        onClick = {
                            val cleanIgn = ign.trim()
                            val cleanUid = gameUid.trim()
                            val cleanPhone = phone.trim()
                            when {
                                cleanIgn.length < 2 -> localError = "IGN must be at least 2 characters."
                                cleanUid.length < 3 -> localError = "Game UID must be at least 3 characters."
                                cleanPhone.length != 10 -> localError = "Enter a valid 10-digit mobile number."
                                else -> onUpdateProfile(cleanIgn, cleanUid, cleanPhone) {}
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
                            .testTag("profile_tab_save_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ArenaObsidian,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.profile_save_changes),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Organizer Support & Logout Card
        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
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
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = "Organizer Support",
                            tint = BlazeOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Organizer Support",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "+91 ${settings.adminPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = EmeraldWin
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91${settings.adminPhone}"))
                                    context.startActivity(dialIntent)
                                }
                            },
                            border = BorderStroke(1.dp, EmeraldWin),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldWin),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "Call Support", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        border = BorderStroke(1.dp, CrimsonReject.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonReject),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout / Switch Google Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
