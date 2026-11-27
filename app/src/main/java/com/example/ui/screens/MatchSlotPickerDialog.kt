package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.GameTitle
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentLudoPairing
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaLightBorder
import com.example.ui.theme.ArenaLightSurface
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.WalletTeal
import com.example.ui.theme.WalletTealDark

@Composable
fun MatchSlotPickerDialog(
    match: TournamentMatch,
    profile: UserProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onGoToWallet: () -> Unit,
    onConfirmSlot: (slotNumber: Long, customIgn: String) -> Unit
) {
    val totalSlots = match.slots.coerceIn(2L, 200L)
    val takenSet = remember(match.id, match.takenSlots, match.joinedCount, totalSlots) {
        if (match.takenSlots.isNotEmpty()) {
            match.takenSlots.toSet()
        } else if (match.joinedCount > 0L) {
            (1L..minOf(match.joinedCount, totalSlots)).toSet()
        } else {
            emptySet()
        }
    }
    val allSlotNumbers = remember(totalSlots) { (1L..totalSlots).toList() }

    var selectedSlot by rememberSaveable(match.id) { mutableStateOf<Long?>(null) }
    var roomIgn by rememberSaveable(match.id) { mutableStateOf(profile.ign.take(30)) }

    // If someone else books the currently selected slot live, clear selection so user picks another open one
    LaunchedEffect(takenSet) {
        val current = selectedSlot
        if (current != null && current in takenSet) {
            selectedSlot = null
        }
    }

    val cleanIgn = roomIgn.trim()
    val isIgnValid = cleanIgn.length in 2..30
    val hasEnoughBalance = profile.wallet >= match.entry
    val shortage = (match.entry - profile.wallet).coerceAtLeast(0L)
    val resolvedFormat = TeamFormat.fromRaw(match.teamFormat).label
    val isLudoTournament = match.game == GameTitle.LUDO.label
    val isTeamSplit = resolvedFormat in listOf("1vs1", "2vs2", "3vs3", "4vs4")
    val teamHalf = (totalSlots / 2L).coerceAtLeast(1L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ArenaWhiteCard)
        ) {
            // Top Purple Header Bar matching screenshot
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ContestPurple)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("slot_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.choose_match_slot),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${match.title} • ${match.joinedCount}/${totalSlots} Booked • Entry ₹${match.entry}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Compact Legend & Team Split Hint
            Surface(
                color = ArenaLightSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SlotLegendItem(
                            boxColor = ArenaWhiteCard,
                            borderColor = ArenaInkDark,
                            checked = false,
                            label = "Open"
                        )
                        SlotLegendItem(
                            boxColor = ArenaInkMuted.copy(alpha = 0.45f),
                            borderColor = Color.Transparent,
                            checked = true,
                            label = "Booked"
                        )
                        SlotLegendItem(
                            boxColor = WalletTealDark,
                            borderColor = WalletTealDark,
                            checked = true,
                            label = "Your Slot"
                        )
                    }
                    if (isLudoTournament) {
                        Text(
                            text = "$resolvedFormat Ludo Boards (#1 vs #${teamHalf + 1}${if (teamHalf > 1L) ", #2 vs #${teamHalf + 2}..." else ""}) • Book any slot now; tap PLAY LUDO at ${match.matchTime}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ContestPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (isTeamSplit) {
                        Text(
                            text = "$resolvedFormat Room Split: Slots 1–$teamHalf = Team 1  •  Slots ${teamHalf + 1}–$totalSlots = Team 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = ContestPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            HorizontalDivider(color = ArenaLightBorder)

            // 4-Column Numbered Slots Grid matching screenshot (1 .. slots)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allSlotNumbers, key = { it }) { slotNum ->
                    val isTaken = slotNum in takenSet
                    val isSelected = selectedSlot == slotNum
                    SlotGridCell(
                        slotNumber = slotNum,
                        isTaken = isTaken,
                        isSelected = isSelected,
                        onSelect = {
                            if (!isTaken) {
                                selectedSlot = if (isSelected) null else slotNum
                            }
                        }
                    )
                }
            }

            // Bottom IGN Input + Confirmation Button Panel
            Surface(
                color = ArenaWhiteCard,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = ArenaLightBorder)

                    OutlinedTextField(
                        value = roomIgn,
                        onValueChange = { roomIgn = it.take(30) },
                        label = {
                            Text(
                                text = "${match.game} In-Game Name (Room Name)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingText = {
                            Text(
                                text = if (roomIgn.isNotEmpty() && !isIgnValid) {
                                    "Enter exact 2–30 character in-game name."
                                } else {
                                    stringResource(R.string.slot_ign_hint)
                                },
                                color = if (roomIgn.isNotEmpty() && !isIgnValid) CrimsonReject else ArenaInkMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = ContestPurple
                            )
                        },
                        isError = roomIgn.isNotEmpty() && !isIgnValid,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ContestPurple,
                            unfocusedBorderColor = ArenaLightBorder,
                            focusedTextColor = ArenaInkDark,
                            unfocusedTextColor = ArenaInkDark,
                            focusedContainerColor = ArenaLightSurface,
                            unfocusedContainerColor = ArenaLightSurface,
                            focusedLabelColor = ContestPurple,
                            unfocusedLabelColor = ArenaInkMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slot_ign_input")
                    )

                    if (!hasEnoughBalance) {
                        Surface(
                            color = BlazeOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BlazeOrange.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Wallet balance ₹${profile.wallet} hai — ₹$shortage aur Add Money karo tab ₹${match.entry} match join hoga.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BlazeOrange,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Button(
                            onClick = onGoToWallet,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldWin,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("slot_go_to_wallet_button")
                        ) {
                            Text(
                                text = stringResource(R.string.go_to_wallet),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val activeSlotChoice = selectedSlot
                        if (isLudoTournament && activeSlotChoice != null) {
                            val myBoard = TournamentLudoPairing.boardForSlot(activeSlotChoice, totalSlots)
                            val oppSlot = TournamentLudoPairing.opponentSlotFor(activeSlotChoice, totalSlots)
                            Text(
                                text = "Slot #$activeSlotChoice plays on Board #$myBoard vs Slot #$oppSlot — match starts automatically whenever both players open Board #$myBoard.",
                                style = MaterialTheme.typography.labelSmall,
                                color = WalletTealDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val canConfirm = selectedSlot != null && isIgnValid && !isBusy
                        Button(
                            onClick = {
                                val slot = selectedSlot
                                if (slot != null && isIgnValid) {
                                    onConfirmSlot(slot, cleanIgn)
                                }
                            },
                            enabled = canConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalletTealDark,
                                contentColor = Color.White,
                                disabledContainerColor = WalletTeal.copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("slot_confirm_button")
                        ) {
                            val buttonLabel = when {
                                selectedSlot == null -> "SELECT AN OPEN SLOT"
                                match.entry == 0L -> "JOIN FREE • SLOT #${selectedSlot}"
                                else -> "NEXT • BOOK SLOT #${selectedSlot} (₹${match.entry})"
                            }
                            Text(
                                text = buttonLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotGridCell(
    slotNumber: Long,
    isTaken: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = when {
        isSelected -> WalletTealDark.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val cellBorder = when {
        isSelected -> BorderStroke(1.5.dp, WalletTealDark)
        else -> null
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        border = cellBorder,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isTaken, onClick = onSelect)
            .testTag("slot_cell_$slotNumber")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = slotNumber.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isSelected -> WalletTealDark
                    isTaken -> ArenaInkMuted
                    else -> ArenaInkDark
                },
                fontWeight = if (isSelected || !isTaken) FontWeight.Bold else FontWeight.Medium
            )
            SlotCheckboxIndicator(
                isTaken = isTaken,
                isSelected = isSelected
            )
        }
    }
}

@Composable
private fun SlotCheckboxIndicator(
    isTaken: Boolean,
    isSelected: Boolean
) {
    val bgColor = when {
        isSelected -> WalletTealDark
        isTaken -> ArenaInkMuted.copy(alpha = 0.45f)
        else -> ArenaWhiteCard
    }
    val border = when {
        isSelected -> null
        isTaken -> null
        else -> BorderStroke(1.8.dp, ArenaInkDark)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        border = border,
        modifier = Modifier.size(22.dp)
    ) {
        if (isTaken || isSelected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = if (isTaken) "Slot booked" else "Slot selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SlotLegendItem(
    boxColor: Color,
    borderColor: Color,
    checked: Boolean,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = boxColor,
            shape = RoundedCornerShape(4.dp),
            border = if (borderColor != Color.Transparent) BorderStroke(1.5.dp, borderColor) else null,
            modifier = Modifier.size(16.dp)
        ) {
            if (checked) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ArenaInkDark,
            fontWeight = FontWeight.Medium
        )
    }
}
