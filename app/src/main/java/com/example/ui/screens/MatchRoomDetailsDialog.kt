package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.MatchMode
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentMatch
import com.example.data.model.parseMatchRuleEntries
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaSlateSurface
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WalletTealDark

private val RoomNoticePurple = Color(0xFFB44CFF)
private val RoomBannerPurple = Color(0xFF7B1FA2)
private val RoomScreenBgTop = Color(0xFF1D243C)
private val RoomScreenBgBottom = Color(0xFF161B2E)

@Composable
fun MatchRoomDetailsDialog(
    match: TournamentMatch,
    alreadyJoined: Boolean,
    myRegistration: MatchRegistration?,
    allMyRegistrations: List<MatchRegistration>,
    matchesById: Map<String, TournamentMatch>,
    onDismiss: () -> Unit,
    onRequestJoin: () -> Unit,
    onCopyFeedback: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showHowToJoinDialog by rememberSaveable { mutableStateOf(false) }
    var showMatchInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showViewEntriesDialog by rememberSaveable { mutableStateOf(false) }
    var showMyEntriesDialog by rememberSaveable { mutableStateOf(false) }

    val isCompleted = MatchStatus.fromRaw(match.status) == MatchStatus.COMPLETED
    val isFull = match.joinedCount >= match.slots
    val resolvedMode = MatchMode.fromRaw(match.matchMode).label
    val resolvedFormat = TeamFormat.fromRaw(match.teamFormat).label
    val bookedSlot = myRegistration?.slotNumber ?: 0L

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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RoomScreenBgTop, RoomScreenBgBottom)
                    )
                )
        ) {
            // Top Dark Header matching screenshot ("<- View More")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("room_details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.view_more_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = ArenaSlateSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${match.game} • $resolvedFormat",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Middle Content: Notices, How To Join, Watermark Emblem + Room ID / Password
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.room_notice_time),
                    style = MaterialTheme.typography.labelLarge,
                    color = RoomNoticePurple,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.room_notice_slot),
                    style = MaterialTheme.typography.labelLarge,
                    color = RoomNoticePurple,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.how_to_join_room),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showHowToJoinDialog = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("how_to_join_room_link")
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (alreadyJoined && myRegistration != null) {
                    Surface(
                        color = EmeraldWinContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = buildString {
                                if (bookedSlot > 0L) append("BOOKED SLOT #$bookedSlot") else append("JOINED MATCH")
                                if (myRegistration.ign.isNotBlank()) append("  •  IGN: ${myRegistration.ign}")
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldWin,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                // Watermark Emblem + Centered Room ID & Password Box matching screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_art),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .alpha(0.22f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (alreadyJoined) {
                            val roomIdDisplay = match.roomId.ifBlank { "WAIT FOR ROOM" }
                            val roomPassDisplay = match.roomPass.ifBlank { "WAIT FOR PASS" }

                            RoomCredentialTapRow(
                                label = "ROOM ID:",
                                value = roomIdDisplay,
                                canCopy = match.roomId.isNotBlank(),
                                onCopy = {
                                    clipboard.setText(AnnotatedString(match.roomId))
                                    onCopyFeedback("Room ID copied!")
                                },
                                testTag = "view_more_copy_room_id"
                            )
                            RoomCredentialTapRow(
                                label = "ROOM PASSWORD:",
                                value = roomPassDisplay,
                                canCopy = match.roomPass.isNotBlank(),
                                onCopy = {
                                    clipboard.setText(AnnotatedString(match.roomPass))
                                    onCopyFeedback("Room Password copied!")
                                },
                                testTag = "view_more_copy_room_pass"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = BlazeOrange,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "ROOM ID: LOCKED",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Join match & select your Slot # to unlock Room ID & Password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom Dock matching screenshot (Teal Split Bar + Purple Status Banner + Teal Bottom Bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoomScreenBgBottom)
                    .navigationBarsPadding()
            ) {
                // Row 1: VIEW MATCH | VIEW ENTRIES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(WalletTealDark)
                ) {
                    BottomDockSegmentButton(
                        text = "VIEW MATCH",
                        onClick = { showMatchInfoDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_view_match_button")
                    )
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.65f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                    )
                    BottomDockSegmentButton(
                        text = "VIEW ENTRIES",
                        onClick = { showViewEntriesDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_view_entries_button")
                    )
                }

                // Purple Live Status Banner
                val bannerHeadline = when {
                    isCompleted -> "Match Completed"
                    alreadyJoined && match.roomId.isNotBlank() -> "Room ID & Password Ready!"
                    alreadyJoined && bookedSlot > 0L -> "Confirmed • Slot #$bookedSlot"
                    alreadyJoined -> "Entry Confirmed"
                    isFull -> "All Slots Full (${match.joinedCount}/${match.slots})"
                    else -> "Book Slot Before Room Fills (${match.joinedCount}/${match.slots})"
                }
                val bannerSubtext = when {
                    isCompleted && match.winnerSummary.isNotBlank() -> "Winners: ${match.winnerSummary}"
                    isCompleted -> "Match finished. Check My Entries or Results for payout details."
                    alreadyJoined && match.roomId.isNotBlank() ->
                        "Hurry Up! Match is starting soon — join in game and sit in Slot #${if (bookedSlot > 0L) bookedSlot else "?"}."
                    alreadyJoined ->
                        "Room ID & Password will display above 4 to 6 minutes before ${match.matchTime}."
                    else ->
                        "Select your numbered slot & ${match.game} In-Game Name to unlock Room ID & Password."
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RoomBannerPurple)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = bannerHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = bannerSubtext,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                }

                // Row 2: MY ENTRIES | JOINED / JOIN NOW
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(WalletTealDark)
                ) {
                    BottomDockSegmentButton(
                        text = "MY ENTRIES",
                        onClick = { showMyEntriesDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_my_entries_button")
                    )
                    VerticalDivider(
                        color = Color.White.copy(alpha = 0.65f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                    )
                    val rightLabel = when {
                        isCompleted -> "COMPLETED"
                        alreadyJoined -> "ALREADY JOINED"
                        isFull -> "SLOTS FULL"
                        else -> "JOIN MATCH (₹${match.entry})"
                    }
                    BottomDockSegmentButton(
                        text = rightLabel,
                        onClick = {
                            if (!alreadyJoined && !isCompleted && !isFull) {
                                onRequestJoin()
                            } else if (alreadyJoined && match.roomId.isNotBlank()) {
                                clipboard.setText(AnnotatedString(match.roomId))
                                onCopyFeedback("Room ID copied!")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("room_bottom_status_button")
                    )
                }
            }
        }
    }

    if (showHowToJoinDialog) {
        AlertDialog(
            onDismissRequest = { showHowToJoinDialog = false },
            containerColor = ArenaSlateDark,
            title = {
                Text("How To Join Custom Room", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Join match in Firebattle and choose your Slot # (e.g. Slot #1) with your exact ${match.game} In-Game Name.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("2. 4–6 minutes before match time, Room ID & Password will appear on this screen.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("3. Tap Copy on Room ID & Password, open Custom Room in game, and enter them.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text("4. Sit ONLY in your booked Slot #. Unknown IGNs or wrong-slot players will be kicked by Admin.", style = MaterialTheme.typography.bodySmall, color = BlazeOrange, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHowToJoinDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ContestPurple)
                ) {
                    Text("Got It", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showMatchInfoDialog) {
        val ruleEntries = remember(match.id, match.rules, match.game) {
            parseMatchRuleEntries(match.rules, match.game)
        }
        AlertDialog(
            onDismissRequest = { showMatchInfoDialog = false },
            containerColor = ArenaSlateDark,
            title = {
                Text(match.title, color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MatchInfoLine("Game", match.game)
                    MatchInfoLine("Mode & Format", "$resolvedMode ($resolvedFormat)")
                    MatchInfoLine("Match Time", match.matchTime)
                    MatchInfoLine("Map", match.mapName)
                    MatchInfoLine("Entry Fee", "₹${match.entry}")
                    MatchInfoLine("Per Kill Reward", "₹${match.perKill}")
                    MatchInfoLine("Prize Pool", "₹${match.prize}")
                    MatchInfoLine("Booked Slots", "${match.joinedCount} / ${match.slots}")
                    HorizontalDivider(
                        color = ArenaSlateBorder,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(
                        text = "TOURNAMENT RULES",
                        style = MaterialTheme.typography.labelMedium,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Bold
                    )
                    ruleEntries.forEach { rule ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (rule.allowed) Icons.Filled.Check else Icons.Filled.Block,
                                contentDescription = null,
                                tint = if (rule.allowed) EmeraldWin else CrimsonReject,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = rule.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rule.allowed) EmeraldWin else TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMatchInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WalletTealDark)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showViewEntriesDialog) {
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
        val slotList = remember(totalSlots) { (1L..totalSlots).toList() }

        AlertDialog(
            onDismissRequest = { showViewEntriesDialog = false },
            containerColor = ArenaSlateDark,
            title = {
                Column {
                    Text("Match Slot Board", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${match.joinedCount}/$totalSlots Booked • Green = Your Slot",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(slotList, key = { it }) { slotNum ->
                        val isMine = bookedSlot == slotNum
                        val isTaken = isMine || (slotNum in takenSet)
                        Surface(
                            color = when {
                                isMine -> EmeraldWinContainer
                                isTaken -> ContestPurple.copy(alpha = 0.25f)
                                else -> ArenaObsidian
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isMine -> EmeraldWin
                                    isTaken -> ContestPurple
                                    else -> ArenaSlateBorder
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "#$slotNum",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isMine) EmeraldWin else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isTaken) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Booked",
                                        tint = if (isMine) EmeraldWin else RoomNoticePurple,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showViewEntriesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WalletTealDark)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showMyEntriesDialog) {
        AlertDialog(
            onDismissRequest = { showMyEntriesDialog = false },
            containerColor = ArenaSlateDark,
            title = {
                Text("My Joined Matches (${allMyRegistrations.size})", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                if (allMyRegistrations.isEmpty()) {
                    Text("You have not joined any matches yet.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allMyRegistrations.forEach { reg ->
                            val linkedMatch = matchesById[reg.matchId]
                            Surface(
                                color = ArenaObsidian,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (reg.matchId == match.id) EmeraldWin else ArenaSlateBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = linkedMatch?.title ?: "Match (${reg.matchId.take(6)})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            if (reg.slotNumber > 0L) append("Slot #${reg.slotNumber} • ")
                                            append("IGN: ${reg.ign} • Paid ₹${reg.entryPaid}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BlazeOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (reg.prizeWon > 0L) {
                                        Text(
                                            text = "🏆 Won ₹${reg.prizeWon} (${reg.kills} Kills)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldWin,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMyEntriesDialog = false }) {
                    Text("Close", color = EmeraldWin, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RoomCredentialTapRow(
    label: String,
    value: String,
    canCopy: Boolean,
    onCopy: () -> Unit,
    testTag: String
) {
    Surface(
        color = ArenaObsidian.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (canCopy) EmeraldWin.copy(alpha = 0.55f) else ArenaSlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = canCopy, onClick = onCopy)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$label ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    color = if (canCopy) Color.White else TextMuted,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canCopy) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = EmeraldWin,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomDockSegmentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MatchInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}
