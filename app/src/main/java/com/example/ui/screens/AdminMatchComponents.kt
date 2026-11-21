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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.MatchMode
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.TeamFormat
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
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class MatchQuickPreset(
    val chipLabel: String,
    val mode: MatchMode,
    val format: TeamFormat,
    val entry: String,
    val prize: String,
    val perKill: String,
    val slots: String,
    val defaultMap: String
)

@Composable
fun CreateTournamentCard(
    isBusy: Boolean,
    onCreateMatch: (
        title: String,
        game: String,
        matchMode: String,
        teamFormat: String,
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
    var selectedMode by rememberSaveable { mutableStateOf(MatchMode.CUSTOM.label) }
    var selectedFormat by rememberSaveable { mutableStateOf(TeamFormat.FULL_MAP.label) }
    var gameMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var time by rememberSaveable { mutableStateOf("") }
    var mapName by rememberSaveable { mutableStateOf("") }
    var entry by rememberSaveable { mutableStateOf("") }
    var perKill by rememberSaveable { mutableStateOf("") }
    var prize by rememberSaveable { mutableStateOf("") }
    var slots by rememberSaveable { mutableStateOf("48") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BlazeOrange,
        unfocusedBorderColor = ArenaSlateBorder,
        focusedContainerColor = ArenaSlateSurface,
        unfocusedContainerColor = ArenaSlateSurface,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    val presets = listOf(
        MatchQuickPreset("1vs1 Lone Wolf", MatchMode.LONE_WOLF, TeamFormat.ONE_VS_ONE, "50", "80", "0", "2", "Iron Cage"),
        MatchQuickPreset("2vs2 CS Duo", MatchMode.CLASH_SQUAD, TeamFormat.TWO_VS_TWO, "50", "160", "0", "4", "Bermuda CS"),
        MatchQuickPreset("4vs4 CS Squad", MatchMode.CLASH_SQUAD, TeamFormat.FOUR_VS_FOUR, "50", "320", "10", "8", "Kalahari CS"),
        MatchQuickPreset("₹20 Custom Room", MatchMode.CUSTOM, TeamFormat.FULL_MAP, "20", "650", "10", "48", "Bermuda"),
        MatchQuickPreset("₹50 Pro Custom", MatchMode.CUSTOM, TeamFormat.FULL_MAP, "50", "1600", "25", "48", "Bermuda")
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
                text = "Quick Mode & Profit Presets (1vs1 / 2vs2 / 4vs4 / Custom)",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = selectedMode == preset.mode.label &&
                        selectedFormat == preset.format.label &&
                        entry == preset.entry &&
                        slots == preset.slots
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedMode = preset.mode.label
                            selectedFormat = preset.format.label
                            entry = preset.entry
                            prize = preset.prize
                            perKill = preset.perKill
                            slots = preset.slots
                            mapName = if (selectedGame == GameTitle.BGMI.label && preset.mode == MatchMode.CUSTOM) {
                                "Erangel"
                            } else {
                                preset.defaultMap
                            }
                            if (time.isBlank()) time = "Tonight 9:00 PM"
                            if (title.isBlank()) title = "${preset.chipLabel} #1"
                        },
                        label = { Text(preset.chipLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldWin,
                            selectedLabelColor = ArenaObsidian,
                            containerColor = ArenaSlateSurface,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            Text(
                text = "Match Mode",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MatchMode.entries) { modeOption ->
                    val selected = selectedMode == modeOption.label
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedMode = modeOption.label
                            when (modeOption) {
                                MatchMode.LONE_WOLF -> {
                                    if (selectedFormat == TeamFormat.FULL_MAP.label || selectedFormat == TeamFormat.FOUR_VS_FOUR.label) {
                                        selectedFormat = TeamFormat.ONE_VS_ONE.label
                                        slots = TeamFormat.ONE_VS_ONE.defaultSlots.toString()
                                    }
                                    if (mapName.isBlank() || mapName == "Bermuda" || mapName == "Erangel") {
                                        mapName = "Iron Cage"
                                    }
                                }
                                MatchMode.CLASH_SQUAD -> {
                                    if (selectedFormat == TeamFormat.FULL_MAP.label) {
                                        selectedFormat = TeamFormat.FOUR_VS_FOUR.label
                                        slots = TeamFormat.FOUR_VS_FOUR.defaultSlots.toString()
                                    }
                                    if (mapName.isBlank() || mapName == "Iron Cage") {
                                        mapName = "Bermuda CS"
                                    }
                                }
                                MatchMode.CUSTOM -> {
                                    if (mapName.isBlank() || mapName == "Iron Cage") {
                                        mapName = if (selectedGame == GameTitle.BGMI.label) "Erangel" else "Bermuda"
                                    }
                                }
                            }
                        },
                        label = { Text(modeOption.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlazeOrange,
                            selectedLabelColor = TextPrimary,
                            containerColor = ArenaSlateSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("admin_mode_chip_${modeOption.name.lowercase()}")
                    )
                }
            }

            Text(
                text = "Team Format & Default Slots",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TeamFormat.entries) { formatOption ->
                    val selected = selectedFormat == formatOption.label
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedFormat = formatOption.label
                            slots = formatOption.defaultSlots.toString()
                        },
                        label = { Text("${formatOption.label} (${formatOption.defaultSlots})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldWin,
                            selectedLabelColor = ArenaObsidian,
                            containerColor = ArenaSlateSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("admin_format_chip_${formatOption.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Match Title (e.g. $selectedMode $selectedFormat #1)") },
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
                    label = { Text("Map (Iron Cage / Bermuda)") },
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
                    label = { Text("Player Slots (2..200)") },
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
                            text = "Organizer Profit Estimator ($selectedMode • $selectedFormat • $slotsNum Slots)",
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
                    onCreateMatch(
                        title,
                        selectedGame,
                        selectedMode,
                        selectedFormat,
                        time,
                        mapName,
                        entry,
                        perKill,
                        prize,
                        slots
                    ) {
                        title = ""
                        time = ""
                        mapName = ""
                        entry = ""
                        perKill = ""
                        prize = ""
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
    val resolvedMode = MatchMode.fromRaw(match.matchMode).label
    val resolvedFormat = TeamFormat.fromRaw(match.teamFormat).label

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
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        item {
                            StatusBadge(status = match.status)
                        }
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
                val isTeamVsMode = resolvedFormat in listOf("1vs1", "2vs2", "4vs4")
                val teamHalf = (match.slots / 2L).coerceAtLeast(1L).toInt()
                val rosterText = if (joinedPlayers.isEmpty()) {
                    "No players joined yet (0/${match.slots} slots filled)"
                } else {
                    joinedPlayers.mapIndexed { idx, reg ->
                        val slotPrefix = if (isTeamVsMode) {
                            val teamNum = if (idx < teamHalf) 1 else 2
                            "Team $teamNum • Slot #${idx + 1}"
                        } else {
                            "Slot #${idx + 1}"
                        }
                        "$slotPrefix: ${reg.ign} (UID: ${reg.gameUid}) • +91 ${reg.phone}"
                    }.joinToString("\n")
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Joined Slots (${joinedPlayers.size}/${match.slots})",
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
