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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LudoPlayMode
import com.example.data.model.LudoRoomStatus
import com.example.data.model.MatchRegistration
import com.example.data.model.TeamFormat
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ludo.LudoEngine
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.ErrorBannerCard
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LudoViewModel

@Composable
fun LudoArenaScreen(
    ludoViewModel: LudoViewModel,
    profile: UserProfile,
    liveLudoMatches: List<TournamentMatch>,
    completedLudoMatches: List<TournamentMatch>,
    joinedIds: Set<String>,
    myRegistrations: List<MatchRegistration>,
    initialFormatFilter: String = "All",
    isMatchBusy: Boolean = false,
    onRequestJoinMatch: (TournamentMatch) -> Unit,
    onInspectMatch: (TournamentMatch) -> Unit,
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

    var showBoardHub by rememberSaveable { mutableStateOf(false) }
    var feedFilter by rememberSaveable { mutableStateOf("Upcoming") }
    var formatFilter by rememberSaveable(initialFormatFilter) { mutableStateOf(initialFormatFilter) }

    LaunchedEffect(initialFormatFilter) {
        formatFilter = initialFormatFilter
    }

    val myRegByMatch = remember(myRegistrations) { myRegistrations.associateBy { it.matchId } }
    val isLocalActive = localState.mode == LudoPlayMode.VS_BOT || localState.mode == LudoPlayMode.PASS_AND_PLAY
    val isOnlineActive = !activeRoomId.isNullOrBlank()

    val navigateBackStep = {
        when {
            isLocalActive -> ludoViewModel.exitLocalMatch()
            isOnlineActive -> ludoViewModel.closeActiveRoom()
            showBoardHub -> showBoardHub = false
            else -> onBackToHome()
        }
    }

    BackHandler { navigateBackStep() }

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
                    onClick = navigateBackStep,
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
                        isOnlineActive -> "In-App Ludo Board"
                        showBoardHub -> "Play In-App Board"
                        else -> "FIREBATTLE LUDO"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!showBoardHub && !isLocalActive && !isOnlineActive) {
                    Surface(
                        color = BlazeOrange,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showBoardHub = true }
                            .testTag("ludo_switch_to_board_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Casino,
                                contentDescription = "Open playable Ludo board",
                                tint = ArenaWhiteCard,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Board",
                                style = MaterialTheme.typography.labelMedium,
                                color = ArenaWhiteCard,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
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
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = EmeraldWin,
                            modifier = Modifier.size(15.dp)
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
        }

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
                                        Text("Back to Board Hub", fontWeight = FontWeight.Bold)
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

            showBoardHub -> LudoBuiltInBoardHub(
                profile = profile,
                waitingRoomsState = waitingRoomsState,
                myRoomsState = myRoomsState,
                isBusy = isBusy,
                onPlayBot = { ludoViewModel.startBotMatch(profile.ign) },
                onPlayPassAndPlay = { ludoViewModel.startPassAndPlayMatch(profile.ign, "Friend") },
                onCreateCashTable = { entry ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to host an online Ludo board.")
                    } else if (entry > profile.wallet) {
                        onCopyFeedback("Insufficient wallet balance (need ₹$entry). Opening Wallet...")
                        onOpenWallet()
                    } else {
                        ludoViewModel.createCashRoom(profile, entry)
                    }
                },
                onJoinByCode = { code ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to join a Ludo board.")
                    } else {
                        ludoViewModel.joinByCode(code, profile)
                    }
                },
                onJoinRoom = { room ->
                    if (!hasInternetConnection(context)) {
                        onCopyFeedback("Turn on internet to join a Ludo board.")
                    } else if (room.entry > profile.wallet) {
                        onCopyFeedback("Need ₹${room.entry} to join this board. Add Money in Wallet.")
                        onOpenWallet()
                    } else {
                        ludoViewModel.joinRoom(room.id, profile)
                    }
                },
                onOpenRoom = { roomId -> ludoViewModel.openOnlineRoom(roomId) },
                onCancelRoom = { roomId -> ludoViewModel.cancelRoom(roomId) }
            )

            else -> LudoSlotsTournamentFeed(
                liveLudoMatches = liveLudoMatches,
                completedLudoMatches = completedLudoMatches,
                joinedIds = joinedIds,
                myRegByMatch = myRegByMatch,
                feedFilter = feedFilter,
                formatFilter = formatFilter,
                isMatchBusy = isMatchBusy,
                onSelectFeed = { feedFilter = it },
                onSelectFormat = { formatFilter = it },
                onJoinMatchClick = onRequestJoinMatch,
                onViewMoreMatchClick = onInspectMatch,
                onOpenBoardHub = { showBoardHub = true }
            )
        }
    }
}

@Composable
private fun LudoSlotsTournamentFeed(
    liveLudoMatches: List<TournamentMatch>,
    completedLudoMatches: List<TournamentMatch>,
    joinedIds: Set<String>,
    myRegByMatch: Map<String, MatchRegistration>,
    feedFilter: String,
    formatFilter: String,
    isMatchBusy: Boolean,
    onSelectFeed: (String) -> Unit,
    onSelectFormat: (String) -> Unit,
    onJoinMatchClick: (TournamentMatch) -> Unit,
    onViewMoreMatchClick: (TournamentMatch) -> Unit,
    onOpenBoardHub: () -> Unit
) {
    val activeSource = if (feedFilter == "Results") completedLudoMatches else liveLudoMatches
    val filteredMatches = remember(activeSource, feedFilter, formatFilter, joinedIds) {
        val byFormat = activeSource.filter { match ->
            val fmt = TeamFormat.fromRaw(match.teamFormat).label
            formatFilter == "All" || fmt == formatFilter
        }
        when (feedFilter) {
            "Ongoing" -> byFormat.filter { it.id in joinedIds || it.roomId.isNotBlank() }
            else -> byFormat
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Purple 3-Tab Strip matching FF/BGMI Tournament Feed
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ContestPurple)
        ) {
            listOf("Ongoing", "Upcoming", "Results").forEach { tabKey ->
                val selected = feedFilter == tabKey
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFeed(tabKey) }
                        .testTag("ludo_feed_${tabKey.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabKey.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selected) Color.White else Color.Transparent)
                    )
                }
            }
        }

        // Dedicated Ludo Format Strip: All | 1vs1 | 2vs2 | 3vs3 | 4vs4
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArenaSlateDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formatOptions = listOf("All", "1vs1", "2vs2", "3vs3", "4vs4")
            items(formatOptions) { fmt ->
                val count = if (fmt == "All") {
                    activeSource.size
                } else {
                    activeSource.count { TeamFormat.fromRaw(it.teamFormat).label == fmt }
                }
                val selected = formatFilter == fmt
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFormat(fmt) },
                    label = {
                        Text(
                            text = if (count > 0) "$fmt ($count)" else fmt,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlazeOrange,
                        selectedLabelColor = ArenaWhiteCard,
                        containerColor = ArenaObsidian,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("ludo_format_chip_$fmt")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filteredMatches.isEmpty()) {
                item {
                    EmptyArenaState(
                        title = when {
                            formatFilter != "All" -> "No $formatFilter Ludo Matches Yet"
                            feedFilter == "Ongoing" -> "No Ongoing Ludo Matches"
                            feedFilter == "Results" -> "No Ludo Results Yet"
                            else -> "No Ludo Slots Open Yet"
                        },
                        subtitle = "Admin can publish Ludo 1vs1, 2vs2, 3vs3 & 4vs4 slot matches from Admin Panel, or try our In-App Ludo Board below."
                    )
                }
            } else {
                items(filteredMatches, key = { it.id }) { match ->
                    TournamentMatchCard(
                        match = match,
                        alreadyJoined = match.id in joinedIds,
                        myRegistration = myRegByMatch[match.id],
                        isBusy = isMatchBusy,
                        onJoinClick = { onJoinMatchClick(match) },
                        onViewMoreClick = { onViewMoreMatchClick(match) }
                    )
                }
            }

            item {
                Surface(
                    color = ArenaSlateDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenBoardHub)
                        .testTag("ludo_practice_board_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "In-App 15×15 Playable Ludo Board",
                                style = MaterialTheme.typography.titleSmall,
                                color = CoinGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play vs Smart Bot, Pass & Play or Room Code",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Casino, contentDescription = null, tint = BlazeOrange)
                    }
                }
            }
        }
    }
}
