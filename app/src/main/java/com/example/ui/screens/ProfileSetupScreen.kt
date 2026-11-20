package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.components.ErrorBannerCard
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileSetupScreen(
    existingProfile: UserProfile?,
    suggestedName: String,
    email: String,
    isBusy: Boolean,
    onSaveProfile: (ign: String, gameUid: String, phone: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ign by rememberSaveable { mutableStateOf(existingProfile?.ign ?: suggestedName) }
    var gameUid by rememberSaveable { mutableStateOf(existingProfile?.gameUid ?: "") }
    var phone by rememberSaveable { mutableStateOf(existingProfile?.phone ?: "") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_setup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = BlazeOrange
                    )
                    Text(
                        text = stringResource(R.string.profile_setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = "Signed in as $email",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldWin
                        )
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlazeOrange,
                        unfocusedBorderColor = ArenaSlateBorder,
                        focusedContainerColor = ArenaSlateSurface,
                        unfocusedContainerColor = ArenaSlateSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )

                    OutlinedTextField(
                        value = ign,
                        onValueChange = { ign = it.take(30); localError = null },
                        label = { Text("In-Game Name (IGN)") },
                        placeholder = { Text("e.g. BlazeSniper", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = BlazeOrange) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_ign_input")
                    )

                    OutlinedTextField(
                        value = gameUid,
                        onValueChange = { gameUid = it.take(30); localError = null },
                        label = { Text("Game UID (Free Fire MAX / BGMI)") },
                        placeholder = { Text("e.g. 2489103421", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = BlazeOrange) },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_uid_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input ->
                            phone = input.filter { it.isDigit() }.take(10)
                            localError = null
                        },
                        label = { Text("10-Digit Mobile Number") },
                        placeholder = { Text("9876543210", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = BlazeOrange) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input")
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
                                else -> onSaveProfile(cleanIgn, cleanUid, cleanPhone)
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
                            .testTag("save_profile_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ArenaObsidian,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save_profile),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        border = BorderStroke(1.dp, CrimsonReject.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonReject),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("profile_setup_logout_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Google Account")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
