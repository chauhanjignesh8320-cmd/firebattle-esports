package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.LudoPlayMode
import com.example.data.model.LudoRoom
import com.example.data.model.LudoRoomStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ludo.LudoEngine
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.hasInternetConnection
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LudoViewModel

@Composable
fun LudoArenaScreen(
    ludoViewModel: LudoViewModel,
    profile: UserProfile,
    onBackToHome: () -> Unit,
    onOpenWallet: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localState by ludoViewModel.localState.collectAsStateWithLifecycle()
    val activeRoomId by ludoViewModel.activeRoomId.collectAsStateWithLifecycle()
    val activeRoomState by ludoViewModel.activeOnlineRoomState.collectAsStateWithLifecycle()
    val waitingRoomsState by ludoViewModel.waitingRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by ludoViewModel.myRoomsState.collectAsStateWithLifecycle()
    val isBusy by ludoViewModel.isBusy.collectAsStateWithLifecycle()

    val isLocalActive = localState.mode == LudoPlayMode.VS_BOT || localState.mode == LudoPlayMode.PASS_AND_PLAY
    val isOnlineActive = !activeRoomId.isNullOrBlank()

    BackHandler {
        when {
            isLocalActive -> ludoViewModel.exitLocalMatch()
            isOnlineActive -> ludoViewModel.closeActiveRoom()
            else -> onBackToHome()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaObsidian)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArenaSlateDark)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = {
                        when {
                            isLocalActive -> ludoViewModel.exitLocalMatch()
                            isOnlineActive -> ludoViewModel.closeActiveRoom()
                            else -> onBackToHome()
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("ludo_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = when {
                        localState.mode == LudoPlayMode.VS_BOT -> "Ludo vs Smart Bot"
                        localState.mode == LudoPlayMode.PASS_AND_PLAY -> "Ludo Pass & Play"
                        isOnlineActive -> "1v1 Online Ludo"
                        else -> "Firebattle Ludo"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = EmeraldWinContainer,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenWallet)
                    .testTag("ludo_wallet_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = EmeraldWin,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "₹${profile.wallet}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldWin,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Body Router
        when {
            isLocalActive -> {
                val winnerName = when (localState.winnerSlot) {
                    0 -> localState.hostName
                    1 -> localState.guestName
                    else -> null
                }
                val isHumanTurn = if (localState.mode == LudoPlayMode.VS_BOT) {
                    localState.turnSlot == 0
                } else {
                    true
                }
                LudoMatchBoardStage(
                    modeBadge = if (localState.mode == LudoPlayMode.VS_BOT) "PRACTICE BOT" else "2P PASS & PLAY",
                    hostName = localState.hostName,
                    guestName = localState.guestName,
                    isBotOpponent = localState.mode == LudoPlayMode.VS_BOT,
                    hostTokens = localState.hostTokens,
                    guestTokens = localState.guestTokens,
                    activeTurnSlot = localState.turnSlot,
                    diceValue = localState.diceValue,
                    awaitingMove = localState.awaitingMove,
                    canRollNow = isHumanTurn && !localState.awaitingMove && winnerName == null,
                    canMoveTokenNow = isHumanTurn && localState.awaitingMove && winnerName == null,
                    isBusy = false,
                    lastAction = localState.lastAction,
                    prizePool = 0L,
                    winnerName = winnerName,
                    isOnlineMatch = false,
                    didLocalUserWin = winnerName != null && (localState.mode == LudoPlayMode.PASS_AND_PLAY || localState.winnerSlot == 0),
                    onRollClick = { ludoViewModel.rollLocalDice() },
                    onTokenSelected = { idx -> ludoViewModel.moveLocalToken(idx) },
                    onForfeitOrExit = { ludoViewModel.exitLocalMatch() },
                    onPlayAgainLocal = {
                        if (localState.mode == LudoPlayMode.VS_BOT) {
                            ludoViewModel.startBotMatch(profile.ign)
                        } else {
                            ludoViewModel.startPassAndPlayMatch(localState.hostName, localState.guestName)
                        }
                    },
                    onReturnLobby = { ludoViewModel.exitLocalMatch() }
                )
            }

            isOnlineActive -> {
                when (val roomState = activeRoomState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BlazeOrange)
                    }

                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        ErrorBannerCard(roomState.message)
                    }

                    is UiState.Success -> {
                        val room = roomState.data
                        if (room == null) {
                            EmptyArenaState(
                                title = "Table Closed",
                                subtitle = "This Ludo room is no longer available."
                            )
                        } else {
                            val status = LudoRoomStatus.fromRaw(room.status)
                            val isHost = room.hostUid == profile.userId
                            when (status) {
                                LudoRoomStatus.WAITING -> LudoOnlineWaitingRoomCard(
                                    room = room,
                                    isHost = isHost,
                                    isBusy = isBusy,
                                    onCancelAndRefund = { ludoViewModel.cancelRoom(room.id) },
                                    onBackToLobby = { ludoViewModel.closeActiveRoom() },
                                    onCopyFeedback = onCopyFeedback
                                )

                                LudoRoomStatus.CANCELLED -> Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    EmptyArenaState(
                                        title = "Table Cancelled",
                                        subtitle = "Host cancelled this table and entry fees were refunded."
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { ludoViewModel.closeActiveRoom() },
                                        colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange)
                                    ) {
                                        Text("Back to Ludo Lobby", fontWeight = FontWeight.Bold)
                                    }
                                }

                                LudoRoomStatus.PLAYING, LudoRoomStatus.COMPLETED -> {
                                    val mySlot = if (isHost) 0 else 1
                                    val isPlaying = status == LudoRoomStatus.PLAYING
                                    val isMyTurn = isPlaying && room.turnSlot.toInt() == mySlot
                                    val winnerName = room.winnerIgn.takeIf { status == LudoRoomStatus.COMPLETED && it.isNotBlank() }

                                    LudoMatchBoardStage(
                                        modeBadge = if (room.entry > 0L) "1v1 ₹${room.entry}" else "1v1 ONLINE FREE",
                                        hostName = room.hostIgn.ifBlank { "Host" },
                                        guestName = room.guestIgn.ifBlank { "Opponent" },
                                        isBotOpponent = false,
                                        hostTokens = LudoEngine.normalizedTokens(room.hostTokens),
                                        guestTokens = LudoEngine.normalizedTokens(room.guestTokens),
                                        activeTurnSlot = room.turnSlot.toInt(),
                                        diceValue = room.diceValue.toInt(),
                                        awaitingMove = room.awaitingMove,
                                        canRollNow = isMyTurn && !room.awaitingMove,
                                        canMoveTokenNow = isMyTurn && room.awaitingMove,
                                        isBusy = isBusy,
                                        lastAction = room.lastAction,
                                        prizePool = room.prizePool,
                                        winnerName = winnerName,
                                        isOnlineMatch = true,
                                        didLocalUserWin = winnerName != null && room.winnerUid == profile.userId,
                                        onRollClick = {
                                            if (!hasInternetConnection(context)) {
                                                onCopyFeedback("No internet connection.")
                                            } else {
                                                ludoViewModel.rollDiceOnline(profile.ign)
                                            }
                                        },
                                        onTokenSelected = { idx ->
                                            if (!hasInternetConnection(context)) {
                                                onCopyFeedback("No internet connection.")
                                            } else {
                                                ludoViewModel.moveTokenOnline(idx, profile.ign)
                                            }
                                        },
                                        onForfeitOrExit = { ludoViewModel.forfeitOnlineRoom() },
                                        onPlayAgainLocal = null,
                                        onReturnLobby = { ludoViewModel.closeActiveRoom() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> LudoLobbyContent(
                profile = profile,
                waitingRoomsState = waitingRoomsState,
                myRoomsState = myRoomsState,
                isBusy = isBusy,
                onPlayBot = { ludoViewModel.startBotMatch(profile.ign) },
                onPlayPassAndPlay = { ludoViewModel.startPassAndPlayMatch(profile.ign, "Friend") },
                onCreateCashTable = { entry ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to host an online Ludo table.")
                    } else if (entry > profile.wallet) {
                        onCopyFeedback("Insufficient wallet balance (need ₹$entry). Opening Wallet...")
                        onOpenWallet()
                    } else {
                        ludoViewModel.createCashRoom(profile, entry)
                    }
                },
                onJoinByCode = { code ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to join a Ludo room.")
                    } else {
                        ludoViewModel.joinByCode(code, profile)
                    }
                },
                onJoinRoom = { room ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to join a Ludo room.")
                    } else if (room.entry > profile.wallet) {
                        onCopyFeedback("Need ₹${room.entry} to join this table. Add Money in Wallet.")
                        onOpenWallet()
                    } else {
                        ludoViewModel.joinRoom(room.id, profile)
                    }
                },
                onOpenRoom = { roomId -> ludoViewModel.openOnlineRoom(roomId) },
                onCancelRoom = { roomId -> ludoViewModel.cancelRoom(roomId) }
            )
        }
    }
}

@Composable
private fun LudoLobbyContent(
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
    var selectedEntry by rememberSaveable { mutableLongStateOf(20L) }
    var customEntryText by rememberSaveable { mutableStateOf("") }
    var joinCodeText by rememberSaveable { mutableStateOf("") }

    val effectiveEntry = customEntryText.toLongOrNull()?.coerceIn(0L, 10_000L) ?: selectedEntry
    val prizePreview = LudoRoom.calculatePrizePool(effectiveEntry)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Artwork Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ArenaSlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tile_ludo),
                        contentDescription = "Firebattle Ludo",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = BlazeOrange,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "1v1 INSTANT LUDO",
                                style = MaterialTheme.typography.labelSmall,
                                color = ArenaWhiteCard,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "Play Cash Battles, Smart Bot\nor 2P Pass & Play",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Instant wallet win credit • Fair live board",
                            style = MaterialTheme.typography.bodySmall,
                            color = CoinGold
                        )
                    }
                }
            }
        }

        // 2. Quick Offline Modes: Smart Bot & Pass and Play
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

        // 3. Create 1v1 Online Cash/Free Table Card
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
                                text = "Host 1v1 Online Table",
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
                                "Create Table (Pay ₹$effectiveEntry • Win ₹$prizePreview)"
                            } else {
                                "Create Free Table"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 4. Join Room by Code
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
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // 5. Open Waiting Tables
        item {
            Text(
                text = "Open Online Tables",
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
                            title = "No Waiting Ludo Tables",
                            subtitle = "Create a table above or play vs Smart Bot while waiting!"
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

        // 6. My Active & Recent Ludo Matches
        val myRooms = (myRoomsState as? UiState.Success)?.data.orEmpty()
        if (myRooms.isNotEmpty()) {
            item {
                Text(
                    text = "My Ludo Matches",
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
