package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LudoRoom
import com.example.data.model.LudoRoomStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LudoBuiltInBoardHub(
    profile: UserProfile,
    waitingRoomsState: UiState<List<LudoRoom>>,
    myRoomsState: UiState<List<LudoRoom>>,
    isBusy: Boolean,
    onPlayBot: () -> Unit,
    onPlayPassAndPlay: () -> Unit,
    onCreateCashTable: (Long) -> Unit,
    onJoinByCode: (String) -> Unit,
    onJoinRoom: (LudoRoom) -> Unit,
    onOpenRoom: (String) -> Unit,
    onCancelRoom: (String) -> Unit
) {
    var selectedEntry by rememberSaveable { mutableLongStateOf(0L) }
    var customEntryText by rememberSaveable { mutableStateOf("") }
    var joinCodeText by rememberSaveable { mutableStateOf("") }

    val effectiveEntry = customEntryText.toLongOrNull()?.coerceIn(0L, 10_000L) ?: selectedEntry
    val prizePreview = LudoRoom.calculatePrizePool(effectiveEntry)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tile_ludo),
                        contentDescription = "Firebattle In-App Ludo Board",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ArenaObsidian.copy(alpha = 0.92f),
                                        ArenaObsidian.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            color = BlazeOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "IN-APP 15×15 LUDO BOARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaWhiteCard,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Play Tournament Room Code, Smart Bot\nor 2P Pass & Play",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = ArenaSlateDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ContestPurple.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onPlayBot)
                        .testTag("ludo_play_bot_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = CoinGold)
                        Text("Play vs Bot", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Free offline AI match", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                Surface(
                    color = ArenaSlateDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onPlayPassAndPlay)
                        .testTag("ludo_pass_and_play_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = EmeraldWin)
                        Text("Pass & Play", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("2 Players same phone", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }

        item {
            Surface(
                color = ArenaSlateDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, tint = BlazeOrange)
                            Text(
                                text = "Host Friend / Room Code Board",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (effectiveEntry > 0L) "Win ₹$prizePreview" else "Free",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldWin,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0L, 10L, 20L, 50L, 100L).forEach { preset ->
                            val selected = customEntryText.isBlank() && selectedEntry == preset
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    customEntryText = ""
                                    selectedEntry = preset
                                },
                                label = {
                                    Text(
                                        text = if (preset == 0L) "Free ₹0" else "₹$preset → ₹${LudoRoom.calculatePrizePool(preset)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BlazeOrange,
                                    selectedLabelColor = ArenaWhiteCard,
                                    containerColor = ArenaObsidian,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("ludo_entry_chip_$preset")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customEntryText,
                        onValueChange = { customEntryText = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Custom Entry ₹ (Optional, 0-10000)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        enabled = !isBusy,
                        onClick = { onCreateCashTable(effectiveEntry) },
                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange, contentColor = ArenaWhiteCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("ludo_create_table_button")
                    ) {
                        Text(
                            text = if (effectiveEntry > 0L) {
                                "Create Board (Pay ₹$effectiveEntry • Win ₹$prizePreview)"
                            } else {
                                "Create Free Board (Get Room Code)"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        item {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = joinCodeText,
                        onValueChange = { joinCodeText = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(10) },
                        label = { Text("Room Code (e.g. LD4K9A)") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ludo_room_code_input")
                    )
                    Button(
                        enabled = !isBusy && joinCodeText.length >= 4,
                        onClick = { onJoinByCode(joinCodeText) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaObsidian),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("ludo_join_code_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Open In-App Boards",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        when (waitingRoomsState) {
            is UiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BlazeOrange)
                }
            }
            is UiState.Error -> item { ErrorBannerCard(waitingRoomsState.message) }
            is UiState.Success -> {
                val tables = waitingRoomsState.data
                if (tables.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = "No Waiting Ludo Boards",
                            subtitle = "Create a room above or play vs Smart Bot!"
                        )
                    }
                } else {
                    items(tables, key = { "wait_${it.id}" }) { room ->
                        val isMine = room.hostUid == profile.userId
                        LudoRoomCard(
                            room = room,
                            isMine = isMine,
                            currentUserId = profile.userId,
                            isBusy = isBusy,
                            onJoin = { onJoinRoom(room) },
                            onOpen = { onOpenRoom(room.id) },
                            onCancel = { onCancelRoom(room.id) }
                        )
                    }
                }
            }
        }

        val myRooms = (myRoomsState as? UiState.Success)?.data.orEmpty()
        if (myRooms.isNotEmpty()) {
            item {
                Text(
                    text = "My Board History",
                    style = MaterialTheme.typography.titleMedium,
                    color = CoinGold,
                    fontWeight = FontWeight.Bold
                )
            }
            items(myRooms.take(10), key = { "mine_${it.id}" }) { room ->
                LudoRoomCard(
                    room = room,
                    isMine = room.hostUid == profile.userId,
                    currentUserId = profile.userId,
                    isBusy = isBusy,
                    onJoin = { onOpenRoom(room.id) },
                    onOpen = { onOpenRoom(room.id) },
                    onCancel = { onCancelRoom(room.id) }
                )
            }
        }
    }
}

@Composable
private fun LudoRoomCard(
    room: LudoRoom,
    isMine: Boolean,
    currentUserId: String,
    isBusy: Boolean,
    onJoin: () -> Unit,
    onOpen: () -> Unit,
    onCancel: () -> Unit
) {
    val status = LudoRoomStatus.fromRaw(room.status)
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (status == LudoRoomStatus.PLAYING) CoinGold.copy(alpha = 0.6f) else ArenaSlateBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ludo_room_card_${room.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = room.roomCode,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = JetBrainsMonoFamily,
                        color = CoinGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (room.entry > 0L) "Entry ₹${room.entry} • Win ₹${room.prizePool}" else "Free Match",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = when (status) {
                        LudoRoomStatus.WAITING -> "Host: ${room.hostIgn} • Waiting"
                        LudoRoomStatus.PLAYING -> "${room.hostIgn} vs ${room.guestIgn} • LIVE"
                        LudoRoomStatus.COMPLETED -> if (room.winnerUid == currentUserId) "You Won${if (room.prizePool > 0L) " ₹${room.prizePool}" else ""}!" else "Winner: ${room.winnerIgn}"
                        LudoRoomStatus.CANCELLED -> "Cancelled & Refunded"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            when (status) {
                LudoRoomStatus.WAITING -> {
                    if (isMine) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                enabled = !isBusy,
                                onClick = onCancel,
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Cancel", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpen,
                                colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text("Open", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            enabled = !isBusy,
                            onClick = onJoin,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin, contentColor = ArenaObsidian),
                            modifier = Modifier
                                .widthIn(min = 88.dp)
                                .heightIn(min = 44.dp)
                                .testTag("ludo_join_room_button_${room.id}")
                        ) {
                            Text(if (room.entry > 0L) "Join ₹${room.entry}" else "Join", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                LudoRoomStatus.PLAYING -> Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = CoinGold, contentColor = ArenaObsidian),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("Resume", fontWeight = FontWeight.ExtraBold)
                }

                LudoRoomStatus.COMPLETED -> StatusBadge(
                    status = if (room.winnerUid == currentUserId) "Approved" else "Rejected"
                )

                LudoRoomStatus.CANCELLED -> StatusBadge(status = "Rejected")
            }
        }
    }
}
