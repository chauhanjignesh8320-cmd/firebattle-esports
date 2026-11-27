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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LudoRoom
import com.example.data.model.LudoRoomStatus
import com.example.ludo.LudoBoardCanvas
import com.example.ludo.LudoDiceControl
import com.example.ludo.LudoEngine
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.ContestPurple
import com.example.ui.theme.CrimsonReject
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LudoOnlineWaitingRoomCard(
    room: LudoRoom,
    isHost: Boolean,
    isBusy: Boolean,
    onCancelAndRefund: () -> Unit,
    onBackToLobby: () -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ArenaSlateDark,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, BlazeOrange.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = BlazeOrange.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = "Ludo Table",
                            tint = BlazeOrange,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        room.isTournamentBoard -> "Board #${room.boardNumber} • Tournament Match"
                        room.entry > 0L -> "1v1 Cash Ludo Table"
                        else -> "1v1 Friendly Ludo Table"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = when {
                        room.isTournamentBoard -> "In-App Auto-Paired Board • No Room Code Needed"
                        room.entry > 0L -> "Entry: ₹${room.entry}  •  Winner Prize: ₹${room.prizePool}"
                        else -> "Free Friendly Match"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = EmeraldWin,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (room.isTournamentBoard) {
                    Surface(
                        color = ArenaObsidian,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You are seated on Board #${room.boardNumber} (${room.hostIgn}). Both players do NOT need to press Play at the exact same second — you can even tap Back below and browse the app. Whenever your opponent taps PLAY LUDO, this board starts live automatically!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {

                Surface(
                    color = ArenaObsidian,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ArenaSlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ROOM CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = CoinGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(room.roomCode))
                                onCopyFeedback("Room code ${room.roomCode} copied!")
                            },
                            border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("ludo_copy_room_code_button")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code", tint = CoinGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = CoinGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = BlazeOrange,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Waiting for an opponent to join...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                if (isHost && !room.isTournamentBoard) {
                    Button(
                        enabled = !isBusy,
                        onClick = onCancelAndRefund,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonReject, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("ludo_cancel_table_button")
                    ) {
                        Text(
                            text = if (room.entry > 0L) "Cancel Table & Refund ₹${room.entry}" else "Cancel Table",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = onBackToLobby,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = if (room.isTournamentBoard) {
                            "Back to Matches (Board Stays Reserved)"
                        } else {
                            "Back to Ludo Lobby (Keep Table Open)"
                        },
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LudoMatchBoardStage(
    modeBadge: String,
    hostName: String,
    guestName: String,
    isBotOpponent: Boolean,
    hostTokens: List<Int>,
    guestTokens: List<Int>,
    activeTurnSlot: Int,
    diceValue: Int,
    awaitingMove: Boolean,
    canRollNow: Boolean,
    canMoveTokenNow: Boolean,
    isBusy: Boolean,
    lastAction: String,
    prizePool: Long,
    winnerName: String?,
    isOnlineMatch: Boolean,
    didLocalUserWin: Boolean?,
    isTournamentBoard: Boolean = false,
    onRollClick: () -> Unit,
    onTokenSelected: (Int) -> Unit,
    onForfeitOrExit: () -> Unit,
    onPlayAgainLocal: (() -> Unit)? = null,
    onReturnLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showForfeitConfirm by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Match mode + Prize pill + Forfeit action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (prizePool > 0L) EmeraldWinContainer else ArenaSlateDark,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (prizePool > 0L) EmeraldWin.copy(alpha = 0.6f) else ArenaSlateBorder)
            ) {
                Text(
                    text = if (prizePool > 0L) "$modeBadge • Win ₹$prizePool" else modeBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (prizePool > 0L) EmeraldWin else CoinGold,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            OutlinedButton(
                enabled = !isBusy,
                onClick = {
                    if (winnerName != null) {
                        onReturnLobby()
                    } else if (isOnlineMatch) {
                        showForfeitConfirm = true
                    } else {
                        onForfeitOrExit()
                    }
                },
                border = BorderStroke(1.dp, if (winnerName != null) EmeraldWin else CrimsonReject.copy(alpha = 0.7f)),
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .testTag("ludo_forfeit_or_exit_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Leave or Forfeit",
                    tint = if (winnerName != null) EmeraldWin else CrimsonReject,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        winnerName != null -> "Lobby"
                        isOnlineMatch -> "Forfeit"
                        else -> "Exit"
                    },
                    color = if (winnerName != null) EmeraldWin else CrimsonReject,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top HUD: Yellow Player (Slot 1: Guest / Bot / Player 2)
        LudoPlayerHudBar(
            name = guestName,
            colorLabel = if (isBotOpponent) "BOT (YELLOW)" else "YELLOW",
            accentColor = Color(0xFFFACC15),
            tokens = guestTokens,
            isActiveTurn = activeTurnSlot == 1 && winnerName == null,
            isBot = isBotOpponent
        )

        // 15x15 Interactive Board
        LudoBoardCanvas(
            hostTokens = hostTokens,
            guestTokens = guestTokens,
            activeTurnSlot = activeTurnSlot,
            diceValue = diceValue,
            awaitingMove = awaitingMove,
            canInteract = canMoveTokenNow && winnerName == null && !isBusy,
            onTokenSelected = onTokenSelected
        )

        // Action Status Ticker
        Surface(
            color = ArenaSlateDark,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ArenaSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = lastAction.ifBlank { "Roll the dice!" },
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        // Bottom HUD: Red Player (Slot 0: Host / Player 1) + Interactive Dice Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LudoPlayerHudBar(
                name = hostName,
                colorLabel = "RED",
                accentColor = Color(0xFFEF4444),
                tokens = hostTokens,
                isActiveTurn = activeTurnSlot == 0 && winnerName == null,
                isBot = false,
                modifier = Modifier.weight(1f)
            )
            LudoDiceControl(
                diceValue = diceValue,
                activeSlot = activeTurnSlot,
                canRoll = canRollNow && winnerName == null,
                awaitingMove = awaitingMove,
                isBusy = isBusy,
                onRollClick = onRollClick
            )
        }

        if (winnerName != null) {
            Surface(
                color = EmeraldWinContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, EmeraldWin),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = CoinGold)
                        Text(
                            text = when {
                                isTournamentBoard -> "$winnerName Won Board!"
                                prizePool > 0L -> "$winnerName Won ₹$prizePool!"
                                else -> "$winnerName Won!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldWin,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (isTournamentBoard) {
                        Text(
                            text = if (didLocalUserWin == true) {
                                "Result recorded! Admin will verify & credit your Tournament Prize."
                            } else {
                                "Board result recorded for Admin verification."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    } else if (prizePool > 0L && didLocalUserWin == true) {
                        Text(
                            text = "₹$prizePool credited to your Winning Wallet!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onPlayAgainLocal != null) {
                            Button(
                                onClick = onPlayAgainLocal,
                                colors = ButtonDefaults.buttonColors(containerColor = BlazeOrange, contentColor = ArenaWhiteCard),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("ludo_play_again_button")
                            ) {
                                Text("Play Again", fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = onReturnLobby,
                            border = BorderStroke(1.dp, EmeraldWin),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("ludo_back_lobby_after_win")
                        ) {
                            Text(
                                text = if (isTournamentBoard) "Back to Matches" else "Ludo Lobby",
                                color = EmeraldWin,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showForfeitConfirm) {
        AlertDialog(
            onDismissRequest = { showForfeitConfirm = false },
            containerColor = ArenaSlateDark,
            title = {
                Text("Forfeit Ludo Match?", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = if (prizePool > 0L) {
                        "If you forfeit now, your opponent automatically wins the ₹$prizePool prize pool."
                    } else {
                        "If you forfeit now, the match will end and your opponent wins."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForfeitConfirm = false
                        onForfeitOrExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonReject, contentColor = Color.White),
                    modifier = Modifier.testTag("ludo_confirm_forfeit_button")
                ) {
                    Text("Forfeit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitConfirm = false }) {
                    Text("Keep Playing", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun LudoPlayerHudBar(
    name: String,
    colorLabel: String,
    accentColor: Color,
    tokens: List<Int>,
    isActiveTurn: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier
) {
    val homeCount = tokens.count { it == LudoEngine.HOME_PROGRESS }
    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isActiveTurn) 2.dp else 1.dp,
            color = if (isActiveTurn) CoinGold else ArenaSlateBorder
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBot) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = name.firstOrNull()?.uppercase() ?: "P",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (accentColor.red > 0.8f && accentColor.green > 0.7f) Color.Black else Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isActiveTurn) "$colorLabel • YOUR TURN" else "$colorLabel • $homeCount/4 Home",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActiveTurn) CoinGold else TextSecondary,
                        fontWeight = if (isActiveTurn) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tokens.forEach { prog ->
                    val dotColor = when {
                        prog == LudoEngine.HOME_PROGRESS -> EmeraldWin
                        prog > 0 -> accentColor
                        else -> ArenaSlateBorder
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveLudoBoardsBanner(
    boards: List<LudoRoom>,
    currentUserId: String,
    onOpenBoard: (LudoRoom) -> Unit,
    modifier: Modifier = Modifier
) {
    if (boards.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        boards.take(3).forEach { room ->
            val status = LudoRoomStatus.fromRaw(room.status)
            val isHost = room.hostUid == currentUserId
            val isParticipant = isHost || room.guestUid == currentUserId
            val mySlot = if (isHost) 0 else 1
            val isMyTurn = status == LudoRoomStatus.PLAYING && isParticipant && room.turnSlot.toInt() == mySlot
            val opponentWaitingForMe = status == LudoRoomStatus.WAITING && !isParticipant && room.isTournamentBoard
            val boardLabel = if (room.isTournamentBoard) "Board #${room.boardNumber}" else "Table ${room.roomCode}"
            val oppIgn = if (isHost) room.guestIgn else room.hostIgn

            val headline = when {
                opponentWaitingForMe -> "${room.hostIgn.ifBlank { "Opponent" }} is waiting on $boardLabel!"
                status == LudoRoomStatus.PLAYING && isMyTurn -> "$boardLabel LIVE • YOUR TURN!"
                status == LudoRoomStatus.PLAYING -> "$boardLabel LIVE • vs ${oppIgn.ifBlank { "Opponent" }}"
                else -> "$boardLabel Reserved • Waiting for opponent"
            }
            val subline = when {
                opponentWaitingForMe -> "Tap Join Board now to start the match."
                status == LudoRoomStatus.PLAYING && isMyTurn -> "Opponent joined — tap Resume to roll your dice!"
                status == LudoRoomStatus.PLAYING -> "Waiting for ${oppIgn.ifBlank { "opponent" }}'s move — tap Resume to watch."
                else -> "You can browse safely; you'll be alerted when opponent joins."
            }
            val btnLabel = when {
                opponentWaitingForMe -> "JOIN BOARD"
                status == LudoRoomStatus.PLAYING -> "RESUME"
                else -> "OPEN"
            }
            val accentColor = when {
                opponentWaitingForMe || isMyTurn -> EmeraldWin
                status == LudoRoomStatus.PLAYING -> CoinGold
                else -> BlazeOrange
            }

            Surface(
                color = if (opponentWaitingForMe || isMyTurn) EmeraldWinContainer else ArenaSlateDark,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenBoard(room) }
                    .testTag("active_ludo_banner_${room.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (opponentWaitingForMe || isMyTurn) Icons.Filled.NotificationsActive else Icons.Filled.Casino,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subline,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { onOpenBoard(room) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (accentColor == BlazeOrange) ArenaWhiteCard else ArenaObsidian
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Text(btnLabel, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }
    }
}

