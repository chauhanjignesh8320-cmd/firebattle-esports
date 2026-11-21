package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GameTitle
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.TournamentMatch
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateTournamentCard(
    isBusy: Boolean,
    onCreateMatch: (
        title: String,
        game: String,
        time: String,
        mapName: String,
        entry: String,
        perKill: String,
        prize: String,
        slots: String,
        onSuccess: () -> Unit
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedGame by rememberSaveable { mutableStateOf(GameTitle.FREE_FIRE_MAX.label) }
    var gameMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var time by rememberSaveable { mutableStateOf("") }
    var mapName by rememberSaveable { mutableStateOf("") }
    var entry by rememberSaveable { mutableStateOf("") }
    var perKill by rememberSaveable { mutableStateOf("") }
    var prize by rememberSaveable { mutableStateOf("") }
    var slots by rememberSaveable { mutableStateOf("") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BlazeOrange,
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "+ Create New Tournament",
                style = MaterialTheme.typography.titleLarge,
                color = BlazeOrange
            )

            Text(
                text = "Quick Profit Presets (Auto 25%+ Owner Margin)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Triple("₹20 Solo", "20", "650") to "48",
                    Triple("₹50 Pro", "50", "1600") to "48",
                    Triple("₹100 VIP", "100", "3200") to "48"
                ).forEach { (triplet, presetSlots) ->
                    val (label, presetEntry, presetPrize) = triplet
                    FilterChip(
                        selected = entry == presetEntry && slots == presetSlots,
                        onClick = {
                            entry = presetEntry
                            prize = presetPrize
                            slots = presetSlots
                            if (perKill.isBlank()) perKill = (presetEntry.toLong() / 2L).toString()
                            if (mapName.isBlank()) mapName = if (selectedGame == GameTitle.BGMI.label) "Erangel" else "Bermuda"
                            if (time.isBlank()) time = "Tonight 9:00 PM"
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldWin,
                            selectedLabelColor = ArenaObsidian,
                            containerColor = ArenaSlateSurface,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Match Title (e.g. Solo Bermuda #1)") },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_match_title_input")
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { gameMenuExpanded = true },
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Game: $selectedGame", color = TextPrimary)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select game", tint = BlazeOrange)
                    }
                }
                DropdownMenu(
                    expanded = gameMenuExpanded,
                    onDismissRequest = { gameMenuExpanded = false }
                ) {
                    GameTitle.entries.forEach { gameOption ->
                        DropdownMenuItem(
                            text = { Text(gameOption.label) },
                            onClick = {
                                selectedGame = gameOption.label
                                gameMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it.take(50) },
                    label = { Text("Time (Today 8:30 PM)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = mapName,
                    onValueChange = { mapName = it.take(40) },
                    label = { Text("Map (Bermuda)") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = entry,
                    onValueChange = { entry = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Entry (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = perKill,
                    onValueChange = { perKill = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Per Kill (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prize,
                    onValueChange = { prize = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Prize Pool (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = slots,
                    onValueChange = { slots = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Slots (2..200)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            val entryNum = entry.toLongOrNull() ?: 0L
            val prizeNum = prize.toLongOrNull() ?: 0L
            val slotsNum = slots.toLongOrNull() ?: 0L
            if (entryNum > 0L && slotsNum > 0L) {
                val maxCollection = entryNum * slotsNum
                val estProfit = maxCollection - prizeNum
                val profitPercent = if (maxCollection > 0L) ((estProfit * 100L) / maxCollection) else 0L
                val profitColor = if (estProfit >= 0L) EmeraldWin else CrimsonReject
                Surface(
                    color = ArenaObsidian,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, profitColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Organizer Profit Estimator ($slotsNum Slots Full)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Collection: ₹$maxCollection",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = if (estProfit >= 0L) {
                                    "Your Profit: +₹$estProfit ($profitPercent%)"
                                } else {
                                    "Loss Warning: -₹${-estProfit}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = profitColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onCreateMatch(title, selectedGame, time, mapName, entry, perKill, prize, slots) {
                        title = ""
                        time = ""
                        mapName = ""
                        entry = ""
                        perKill = ""
                        prize = ""
                        slots = ""
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlazeOrange,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("publish_match_button")
            ) {
                Text(stringResource(R.string.publish_match), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminMatchCard(
    match: TournamentMatch,
    joinedPlayers: List<MatchRegistration>,
    isBusy: Boolean,
    onUpdateRoom: (roomId: String, roomPass: String) -> Unit,
    onToggleStatus: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var roomId by rememberSaveable(match.id, match.roomId) { mutableStateOf(match.roomId) }
    var roomPass by rememberSaveable(match.id, match.roomPass) { mutableStateOf(match.roomPass) }

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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        StatusBadge(status = match.status)
                    }
                    Text(
                        text = match.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "${match.matchTime} • Map: ${match.mapName} • Entry: ₹${match.entry} • Slots: ${match.joinedCount}/${match.slots}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    val collected = match.entry * match.joinedCount
                    val matchNet = collected - match.prize
                    Text(
                        text = "Collected: ₹$collected • Prize: ₹${match.prize} • Net Margin: ${if (matchNet >= 0) "+₹$matchNet" else "-₹${-matchNet}"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (matchNet >= 0) EmeraldWin else BlazeOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Match",
                        tint = CrimsonReject
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { roomId = it.take(40) },
                    label = { Text("Room ID") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = roomPass,
                    onValueChange = { roomPass = it.take(40) },
                    label = { Text("Room Pass") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onUpdateRoom(roomId, roomPass) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldWin,
                        contentColor = ArenaObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("UPDATE ROOM", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onToggleStatus,
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (MatchStatus.fromRaw(match.status) == MatchStatus.LIVE) "Complete" else "Reopen",
                        color = TextPrimary
                    )
                }
            }

            Surface(
                color = ArenaObsidian,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                val rosterText = if (joinedPlayers.isEmpty()) {
                    "No players joined yet"
                } else {
                    joinedPlayers.joinToString("\n") { "${it.ign} (UID: ${it.gameUid}) • +91 ${it.phone}" }
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Joined Players (${joinedPlayers.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = BlazeOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rosterText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
