package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.LudoRepository
import com.example.data.model.LudoPlayMode
import com.example.data.model.LudoRoom
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ludo.LudoEngine
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocalLudoState(
    val mode: LudoPlayMode = LudoPlayMode.LOBBY,
    val hostName: String = "You",
    val guestName: String = "FireBot",
    val turnSlot: Int = 0,
    val diceValue: Int = 0,
    val awaitingMove: Boolean = false,
    val consecutiveSixes: Int = 0,
    val hostTokens: List<Int> = listOf(0, 0, 0, 0),
    val guestTokens: List<Int> = listOf(0, 0, 0, 0),
    val lastAction: String = "Roll the dice to begin!",
    val winnerSlot: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class LudoViewModel(
    private val repository: LudoRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _localState = MutableStateFlow(LocalLudoState())
    val localState: StateFlow<LocalLudoState> = _localState.asStateFlow()

    private var botTurnJob: Job? = null

    val waitingRoomsState: StateFlow<UiState<List<LudoRoom>>> =
        repository.observeOpenWaitingRooms(currentUserId)
            .toUiState("open Ludo tables")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myRoomsState: StateFlow<UiState<List<LudoRoom>>> =
        combine(
            repository.observeMyHostedRooms(currentUserId),
            repository.observeMyJoinedRooms(currentUserId)
        ) { hosted, joined ->
            (hosted + joined)
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
            .toUiState("your Ludo matches")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val activeOnlineRoomState: StateFlow<UiState<LudoRoom?>> =
        _activeRoomId
            .flatMapLatest { roomId ->
                if (roomId.isNullOrBlank()) {
                    flowOf(UiState.Success(null))
                } else {
                    repository.observeRoom(roomId, currentUserId).toUiState("Ludo room")
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Success(null))

    fun openOnlineRoom(roomId: String) {
        botTurnJob?.cancel()
        _localState.value = LocalLudoState(mode = LudoPlayMode.LOBBY)
        _activeRoomId.value = roomId
    }

    fun closeActiveRoom() {
        _activeRoomId.value = null
    }

    fun startBotMatch(playerIgn: String) {
        botTurnJob?.cancel()
        _activeRoomId.value = null
        val cleanName = playerIgn.trim().ifBlank { "You" }.take(20)
        _localState.value = LocalLudoState(
            mode = LudoPlayMode.VS_BOT,
            hostName = cleanName,
            guestName = "FireBot",
            turnSlot = 0,
            diceValue = 0,
            awaitingMove = false,
            consecutiveSixes = 0,
            hostTokens = listOf(0, 0, 0, 0),
            guestTokens = listOf(0, 0, 0, 0),
            lastAction = "Practice Match started! Roll dice ($cleanName's turn).",
            winnerSlot = null
        )
    }

    fun startPassAndPlayMatch(playerOneIgn: String, playerTwoIgn: String = "Player 2") {
        botTurnJob?.cancel()
        _activeRoomId.value = null
        val p1 = playerOneIgn.trim().ifBlank { "Player 1" }.take(20)
        val p2 = playerTwoIgn.trim().ifBlank { "Player 2" }.take(20)
        _localState.value = LocalLudoState(
            mode = LudoPlayMode.PASS_AND_PLAY,
            hostName = p1,
            guestName = p2,
            turnSlot = 0,
            diceValue = 0,
            awaitingMove = false,
            consecutiveSixes = 0,
            hostTokens = listOf(0, 0, 0, 0),
            guestTokens = listOf(0, 0, 0, 0),
            lastAction = "2P Pass & Play started! $p1 rolls first.",
            winnerSlot = null
        )
    }

    fun exitLocalMatch() {
        botTurnJob?.cancel()
        _localState.value = LocalLudoState(mode = LudoPlayMode.LOBBY)
    }

    fun rollLocalDice() {
        val state = _localState.value
        if (state.mode != LudoPlayMode.VS_BOT && state.mode != LudoPlayMode.PASS_AND_PLAY) return
        if (state.winnerSlot != null || state.awaitingMove) return
        if (state.mode == LudoPlayMode.VS_BOT && state.turnSlot != 0) return

        val activeName = if (state.turnSlot == 0) state.hostName else state.guestName
        val dice = (1..6).random()
        val nextSixes = if (dice == 6) state.consecutiveSixes + 1 else 0

        if (nextSixes >= 3) {
            val nextTurn = 1 - state.turnSlot
            _localState.value = state.copy(
                turnSlot = nextTurn,
                diceValue = dice,
                awaitingMove = false,
                consecutiveSixes = 0,
                lastAction = "$activeName rolled three 6s! Turn passed."
            )
            if (state.mode == LudoPlayMode.VS_BOT && nextTurn == 1) {
                scheduleBotTurn()
            }
            return
        }

        val activeTokens = if (state.turnSlot == 0) state.hostTokens else state.guestTokens
        val valid = LudoEngine.validTokenIndices(activeTokens, dice)
        if (valid.isEmpty()) {
            val nextTurn = 1 - state.turnSlot
            _localState.value = state.copy(
                turnSlot = nextTurn,
                diceValue = dice,
                awaitingMove = false,
                consecutiveSixes = 0,
                lastAction = "$activeName rolled $dice (no valid move)."
            )
            if (state.mode == LudoPlayMode.VS_BOT && nextTurn == 1) {
                scheduleBotTurn()
            }
        } else {
            _localState.value = state.copy(
                diceValue = dice,
                awaitingMove = true,
                consecutiveSixes = nextSixes,
                lastAction = "$activeName rolled $dice! Tap a highlighted token."
            )
        }
    }

    fun moveLocalToken(tokenIndex: Int) {
        val state = _localState.value
        if (state.mode != LudoPlayMode.VS_BOT && state.mode != LudoPlayMode.PASS_AND_PLAY) return
        if (state.winnerSlot != null || !state.awaitingMove) return
        if (state.mode == LudoPlayMode.VS_BOT && state.turnSlot != 0) return

        val activeSlot = state.turnSlot
        val activeName = if (activeSlot == 0) state.hostName else state.guestName
        val activeTokens = if (activeSlot == 0) state.hostTokens else state.guestTokens
        val opponentTokens = if (activeSlot == 0) state.guestTokens else state.hostTokens
        if (tokenIndex !in LudoEngine.validTokenIndices(activeTokens, state.diceValue)) return

        val outcome = LudoEngine.applyMove(
            activeTokens = activeTokens,
            opponentTokens = opponentTokens,
            tokenIndex = tokenIndex,
            dice = state.diceValue,
            activeSlot = activeSlot,
            activeName = activeName
        )

        val nextHost = if (activeSlot == 0) outcome.nextActiveTokens else outcome.nextOpponentTokens
        val nextGuest = if (activeSlot == 1) outcome.nextActiveTokens else outcome.nextOpponentTokens
        val nextSixes = if (outcome.extraTurn && state.diceValue == 6) state.consecutiveSixes else 0

        _localState.value = state.copy(
            hostTokens = nextHost,
            guestTokens = nextGuest,
            turnSlot = outcome.nextTurnSlot,
            awaitingMove = false,
            consecutiveSixes = nextSixes,
            lastAction = outcome.summary,
            winnerSlot = if (outcome.didWin) activeSlot else null
        )

        if (state.mode == LudoPlayMode.VS_BOT && !outcome.didWin && outcome.nextTurnSlot == 1) {
            scheduleBotTurn()
        }
    }

    private fun scheduleBotTurn() {
        botTurnJob?.cancel()
        botTurnJob = viewModelScope.launch {
            while (true) {
                val cur = _localState.value
                if (cur.mode != LudoPlayMode.VS_BOT || cur.turnSlot != 1 || cur.winnerSlot != null) {
                    break
                }
                delay(650L)
                val fresh = _localState.value
                if (fresh.mode != LudoPlayMode.VS_BOT || fresh.turnSlot != 1 || fresh.winnerSlot != null) {
                    break
                }
                val dice = (1..6).random()
                val nextSixes = if (dice == 6) fresh.consecutiveSixes + 1 else 0
                if (nextSixes >= 3) {
                    _localState.value = fresh.copy(
                        turnSlot = 0,
                        diceValue = dice,
                        awaitingMove = false,
                        consecutiveSixes = 0,
                        lastAction = "${fresh.guestName} rolled three 6s! Your turn."
                    )
                    break
                }
                val valid = LudoEngine.validTokenIndices(fresh.guestTokens, dice)
                if (valid.isEmpty()) {
                    _localState.value = fresh.copy(
                        turnSlot = 0,
                        diceValue = dice,
                        awaitingMove = false,
                        consecutiveSixes = 0,
                        lastAction = "${fresh.guestName} rolled $dice (no valid move). Your turn!"
                    )
                    break
                }

                _localState.value = fresh.copy(
                    diceValue = dice,
                    awaitingMove = true,
                    consecutiveSixes = nextSixes,
                    lastAction = "${fresh.guestName} rolled $dice..."
                )
                delay(650L)

                val preMove = _localState.value
                if (preMove.mode != LudoPlayMode.VS_BOT || preMove.turnSlot != 1 || preMove.winnerSlot != null) {
                    break
                }
                val chosen = LudoEngine.chooseBotToken(preMove.guestTokens, preMove.hostTokens, dice) ?: valid.first()
                val outcome = LudoEngine.applyMove(
                    activeTokens = preMove.guestTokens,
                    opponentTokens = preMove.hostTokens,
                    tokenIndex = chosen,
                    dice = dice,
                    activeSlot = 1,
                    activeName = preMove.guestName
                )
                _localState.value = preMove.copy(
                    hostTokens = outcome.nextOpponentTokens,
                    guestTokens = outcome.nextActiveTokens,
                    turnSlot = outcome.nextTurnSlot,
                    awaitingMove = false,
                    consecutiveSixes = if (outcome.extraTurn && dice == 6) nextSixes else 0,
                    lastAction = outcome.summary,
                    winnerSlot = if (outcome.didWin) 1 else null
                )
                if (outcome.didWin || outcome.nextTurnSlot != 1) {
                    break
                }
            }
        }
    }

    fun createCashRoom(profile: UserProfile, entry: Long) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.createLudoRoom(profile, entry)
                res.onSuccess { roomId ->
                    openOnlineRoom(roomId)
                    _events.emit(
                        if (entry > 0L) "Ludo ₹$entry table live! Share code or wait for opponent."
                        else "Free Ludo table live! Share code with a friend."
                    )
                }.onFailure { err ->
                    _events.emit(err.message ?: "Unable to create Ludo table.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun cancelRoom(roomId: String) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.cancelWaitingRoom(roomId)
                res.onSuccess {
                    if (_activeRoomId.value == roomId) {
                        _activeRoomId.value = null
                    }
                    _events.emit("Ludo table cancelled & entry fee refunded!")
                }.onFailure { err ->
                    _events.emit(err.message ?: "Could not cancel table.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun joinRoom(roomId: String, profile: UserProfile) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.joinLudoRoom(roomId, profile)
                res.onSuccess {
                    openOnlineRoom(roomId)
                    _events.emit("Joined Ludo table! Red (Host) rolls first.")
                }.onFailure { err ->
                    _events.emit(err.message ?: "Failed to join table.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun joinByCode(code: String, profile: UserProfile, onSuccess: () -> Unit = {}) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.joinRoomByCode(code, profile)
                res.onSuccess { roomId ->
                    openOnlineRoom(roomId)
                    _events.emit("Joined Ludo table! Match is live.")
                    onSuccess()
                }.onFailure { err ->
                    _events.emit(err.message ?: "Room code not found.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun rollDiceOnline(callerIgn: String) {
        val roomId = _activeRoomId.value ?: return
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.rollDiceOnline(roomId, callerIgn)
                res.onFailure { err ->
                    _events.emit(err.message ?: "Failed to roll dice.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun moveTokenOnline(tokenIndex: Int, callerIgn: String) {
        val roomId = _activeRoomId.value ?: return
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.moveTokenOnline(roomId, tokenIndex, callerIgn)
                res.onFailure { err ->
                    _events.emit(err.message ?: "Failed to move token.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun forfeitOnlineRoom() {
        val roomId = _activeRoomId.value ?: return
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val res = repository.forfeitOnlineMatch(roomId)
                res.onSuccess {
                    _events.emit("Match forfeited.")
                }.onFailure { err ->
                    _events.emit(err.message ?: "Could not forfeit match.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Stream error loading $label", error)
                val friendly = if (error is FirebaseFirestoreException) {
                    "Sync error loading $label. Please check your connection and try again."
                } else {
                    error.message ?: "Unable to load $label."
                }
                emit(UiState.Error(friendly))
            }

    private companion object {
        const val TAG = "LudoViewModel"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
