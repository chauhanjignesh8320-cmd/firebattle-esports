package com.example.data.firebase

import com.example.data.model.LudoRoom
import com.example.data.model.LudoRoomStatus
import com.example.data.model.MatchRegistration
import com.example.data.model.TournamentLudoPairing
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.ludo.LudoEngine
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.min

class LudoRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before playing online Ludo.")
    }

    private fun handleStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    fun observeOpenWaitingRooms(uid: String): Flow<List<LudoRoom>> = flow {
        val path = "ludoRooms"
        emitAll(
            db.collection("ludoRooms")
                .whereEqualTo("status", LudoRoomStatus.WAITING.wireValue)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeMyHostedRooms(uid: String): Flow<List<LudoRoom>> = flow {
        val path = "ludoRooms"
        emitAll(
            db.collection("ludoRooms")
                .whereEqualTo("hostUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeMyJoinedRooms(uid: String): Flow<List<LudoRoom>> = flow {
        val path = "ludoRooms"
        emitAll(
            db.collection("ludoRooms")
                .whereEqualTo("guestUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeRoom(roomId: String, uid: String): Flow<LudoRoom?> = flow {
        val path = "ludoRooms/$roomId"
        emitAll(
            db.collection("ludoRooms")
                .document(roomId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeMatchBoards(matchId: String, uid: String): Flow<List<LudoRoom>> = flow {
        val path = "ludoRooms"
        emitAll(
            db.collection("ludoRooms")
                .whereEqualTo("matchId", matchId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.boardNumber }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllTournamentBoards(uid: String): Flow<List<LudoRoom>> = flow {
        val path = "ludoRooms"
        emitAll(
            db.collection("ludoRooms")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.matchId.isNotBlank() }
                        .sortedBy { it.boardNumber }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun openOrJoinTournamentBoard(
        match: TournamentMatch,
        registration: MatchRegistration,
        profile: UserProfile
    ): Result<String> {
        var path = "ludoRooms"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed." }
            require(registration.matchId == match.id) { "Registration does not belong to this match." }
            require(registration.slotNumber in 1L..match.slots) { "Please book a valid slot first." }

            val boardNumber = TournamentLudoPairing.boardForSlot(registration.slotNumber, match.slots)
            val opponentSlot = TournamentLudoPairing.opponentSlotFor(registration.slotNumber, match.slots)
            val roomId = TournamentLudoPairing.roomDocId(match.id, boardNumber)
            val roomRef = db.collection("ludoRooms").document(roomId)
            path = roomRef.path

            val cleanIgn = registration.ign.trim().ifBlank { profile.ign.trim() }.take(30).ifBlank { "Player" }
            val roomCode = "TBL" + boardNumber.toString().padStart(2, '0')

            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                if (snap.exists()) {
                    val existing = snap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Invalid board.")
                    when {
                        existing.hostUid == uid || existing.guestUid == uid -> Unit
                        existing.status == LudoRoomStatus.WAITING.wireValue -> {
                            tx.update(
                                roomRef,
                                mapOf(
                                    "guestUid" to uid,
                                    "guestIgn" to cleanIgn,
                                    "status" to LudoRoomStatus.PLAYING.wireValue,
                                    "turnSlot" to 0L,
                                    "diceValue" to 0L,
                                    "awaitingMove" to false,
                                    "consecutiveSixes" to 0L,
                                    "lastAction" to "$cleanIgn joined Board #$boardNumber! ${existing.hostIgn}'s turn.",
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        else -> throw IllegalStateException("Board #$boardNumber is already full or completed.")
                    }
                } else {
                    tx.set(
                        roomRef,
                        mapOf(
                            "roomCode" to roomCode,
                            "hostUid" to uid,
                            "hostIgn" to cleanIgn,
                            "guestUid" to "",
                            "guestIgn" to "",
                            "entry" to 0L,
                            "prizePool" to 0L,
                            "status" to LudoRoomStatus.WAITING.wireValue,
                            "turnSlot" to 0L,
                            "diceValue" to 0L,
                            "awaitingMove" to false,
                            "consecutiveSixes" to 0L,
                            "hostTokens" to listOf(0L, 0L, 0L, 0L),
                            "guestTokens" to listOf(0L, 0L, 0L, 0L),
                            "lastAction" to "Board #$boardNumber ready! Waiting for Slot #$opponentSlot.",
                            "winnerUid" to "",
                            "winnerIgn" to "",
                            "forfeitByUid" to "",
                            "prizePaid" to false,
                            "matchId" to match.id,
                            "boardNumber" to boardNumber,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                Unit
            }.await()

            Result.success(roomId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createLudoRoom(profile: UserProfile, entry: Long): Result<String> {
        var path = "ludoRooms"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed." }
            require(entry in 0L..10_000L) { "Ludo entry fee must be between ₹0 and ₹10,000." }

            val prizePool = LudoRoom.calculatePrizePool(entry)
            val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val roomCode = "LD" + (1..4).map { codeChars.random() }.joinToString("")
            val roomRef = db.collection("ludoRooms").document()
            val userRef = db.collection("users").document(uid)
            path = roomRef.path

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val currentWallet = userSnap.getLong("wallet") ?: 0L
                val currentWinning = userSnap.getLong("winningBalance") ?: 0L
                if (entry > 0L && currentWallet < entry) {
                    throw IllegalStateException("Insufficient wallet balance (need ₹$entry, have ₹$currentWallet).")
                }

                if (entry > 0L) {
                    val nextWallet = currentWallet - entry
                    val nextWinning = min(currentWinning, nextWallet).coerceAtLeast(0L)
                    tx.update(
                        userRef,
                        mapOf(
                            "wallet" to nextWallet,
                            "winningBalance" to nextWinning,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    roomRef,
                    mapOf(
                        "roomCode" to roomCode,
                        "hostUid" to uid,
                        "hostIgn" to profile.ign.trim().take(30).ifBlank { "Player" },
                        "guestUid" to "",
                        "guestIgn" to "",
                        "entry" to entry,
                        "prizePool" to prizePool,
                        "status" to LudoRoomStatus.WAITING.wireValue,
                        "turnSlot" to 0L,
                        "diceValue" to 0L,
                        "awaitingMove" to false,
                        "consecutiveSixes" to 0L,
                        "hostTokens" to listOf(0L, 0L, 0L, 0L),
                        "guestTokens" to listOf(0L, 0L, 0L, 0L),
                        "lastAction" to if (entry > 0L) "Table opened (Entry ₹$entry • Win ₹$prizePool)" else "Free friendly Ludo table opened",
                        "winnerUid" to "",
                        "winnerIgn" to "",
                        "forfeitByUid" to "",
                        "prizePaid" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Unit
            }.await()

            Result.success(roomRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelWaitingRoom(roomId: String): Result<Unit> {
        val path = "ludoRooms/$roomId"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("ludoRooms").document(roomId)
            val userRef = db.collection("users").document(uid)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                if (!roomSnap.exists()) throw IllegalStateException("Ludo table no longer exists.")
                val room = roomSnap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Invalid room.")
                if (room.hostUid != uid) throw IllegalStateException("Only the host can cancel this table.")
                if (room.status != LudoRoomStatus.WAITING.wireValue) {
                    throw IllegalStateException("Match already started or closed.")
                }

                if (room.entry > 0L) {
                    val userSnap = tx.get(userRef)
                    val currentWallet = userSnap.getLong("wallet") ?: 0L
                    tx.update(
                        userRef,
                        mapOf(
                            "wallet" to (currentWallet + room.entry),
                            "lastLudoRoomId" to roomId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.update(
                    roomRef,
                    mapOf(
                        "status" to LudoRoomStatus.CANCELLED.wireValue,
                        "lastAction" to "Cancelled by host & refunded",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun joinLudoRoom(roomId: String, profile: UserProfile): Result<Unit> {
        val path = "ludoRooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed." }
            val roomRef = db.collection("ludoRooms").document(roomId)
            val userRef = db.collection("users").document(uid)

            db.runTransaction { tx ->
                val roomSnap = tx.get(roomRef)
                if (!roomSnap.exists()) throw IllegalStateException("Ludo room not found.")
                val room = roomSnap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Invalid room.")
                if (room.status != LudoRoomStatus.WAITING.wireValue) {
                    throw IllegalStateException("This Ludo table is already full or closed.")
                }
                if (room.hostUid == uid) {
                    throw IllegalStateException("You cannot join your own table.")
                }

                if (room.entry > 0L) {
                    val userSnap = tx.get(userRef)
                    val currentWallet = userSnap.getLong("wallet") ?: 0L
                    val currentWinning = userSnap.getLong("winningBalance") ?: 0L
                    if (currentWallet < room.entry) {
                        throw IllegalStateException("Insufficient balance (need ₹${room.entry}, have ₹$currentWallet).")
                    }
                    val nextWallet = currentWallet - room.entry
                    val nextWinning = min(currentWinning, nextWallet).coerceAtLeast(0L)
                    tx.update(
                        userRef,
                        mapOf(
                            "wallet" to nextWallet,
                            "winningBalance" to nextWinning,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val guestIgn = profile.ign.trim().take(30).ifBlank { "Player" }
                tx.update(
                    roomRef,
                    mapOf(
                        "guestUid" to uid,
                        "guestIgn" to guestIgn,
                        "status" to LudoRoomStatus.PLAYING.wireValue,
                        "turnSlot" to 0L,
                        "diceValue" to 0L,
                        "awaitingMove" to false,
                        "consecutiveSixes" to 0L,
                        "lastAction" to "$guestIgn joined! ${room.hostIgn}'s turn.",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(rawCode: String, profile: UserProfile): Result<String> {
        val cleanCode = rawCode.trim().uppercase().filter { it.isLetterOrDigit() }
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Enter a valid Ludo room code (e.g. LD1234)."))
        }
        return try {
            requireUserId()
            val waitingDocs = db.collection("ludoRooms")
                .whereEqualTo("status", LudoRoomStatus.WAITING.wireValue)
                .get()
                .await()
                .toObjects(LudoRoom::class.java)
            val target = waitingDocs.firstOrNull { it.roomCode.equals(cleanCode, ignoreCase = true) }
                ?: return Result.failure(IllegalStateException("No waiting Ludo room found with code $cleanCode."))
            joinLudoRoom(target.id, profile).map { target.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "ludoRooms")
            Result.failure(e)
        }
    }

    suspend fun rollDiceOnline(roomId: String, callerIgn: String): Result<Unit> {
        val path = "ludoRooms/$roomId"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("ludoRooms").document(roomId)

            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val room = snap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Room not found.")
                if (room.status != LudoRoomStatus.PLAYING.wireValue) {
                    throw IllegalStateException("Match is not active.")
                }
                val activeSlot = room.turnSlot.toInt()
                val activeUid = if (activeSlot == 0) room.hostUid else room.guestUid
                if (activeUid != uid) throw IllegalStateException("Wait for your turn.")
                if (room.awaitingMove) throw IllegalStateException("Pick a highlighted token to move first.")

                val dice = (1..6).random()
                val nextSixes = if (dice == 6) (room.consecutiveSixes.toInt() + 1).coerceAtMost(3) else 0
                val hostTokens = LudoEngine.normalizedTokens(room.hostTokens)
                val guestTokens = LudoEngine.normalizedTokens(room.guestTokens)
                val activeTokens = if (activeSlot == 0) hostTokens else guestTokens

                if (nextSixes >= 3) {
                    tx.update(
                        roomRef,
                        mapOf(
                            "turnSlot" to (1 - activeSlot).toLong(),
                            "diceValue" to dice.toLong(),
                            "awaitingMove" to false,
                            "consecutiveSixes" to 0L,
                            "lastAction" to "$callerIgn rolled three 6s! Turn passed.",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    return@runTransaction Unit
                }

                val validMoves = LudoEngine.validTokenIndices(activeTokens, dice)
                if (validMoves.isEmpty()) {
                    tx.update(
                        roomRef,
                        mapOf(
                            "turnSlot" to (1 - activeSlot).toLong(),
                            "diceValue" to dice.toLong(),
                            "awaitingMove" to false,
                            "consecutiveSixes" to 0L,
                            "lastAction" to "$callerIgn rolled $dice (no valid move).",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.update(
                        roomRef,
                        mapOf(
                            "turnSlot" to activeSlot.toLong(),
                            "diceValue" to dice.toLong(),
                            "awaitingMove" to true,
                            "consecutiveSixes" to nextSixes.toLong(),
                            "lastAction" to "$callerIgn rolled $dice! Tap a glowing token.",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun moveTokenOnline(roomId: String, tokenIndex: Int, callerIgn: String): Result<Unit> {
        val path = "ludoRooms/$roomId"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("ludoRooms").document(roomId)

            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val room = snap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Room not found.")
                if (room.status != LudoRoomStatus.PLAYING.wireValue) {
                    throw IllegalStateException("Match is not active.")
                }
                val activeSlot = room.turnSlot.toInt()
                val activeUid = if (activeSlot == 0) room.hostUid else room.guestUid
                if (activeUid != uid) throw IllegalStateException("Wait for your turn.")
                if (!room.awaitingMove) throw IllegalStateException("Roll the dice first.")

                val dice = room.diceValue.toInt()
                val hostTokens = LudoEngine.normalizedTokens(room.hostTokens)
                val guestTokens = LudoEngine.normalizedTokens(room.guestTokens)
                val activeTokens = if (activeSlot == 0) hostTokens else guestTokens
                val opponentTokens = if (activeSlot == 0) guestTokens else hostTokens

                val validMoves = LudoEngine.validTokenIndices(activeTokens, dice)
                if (tokenIndex !in validMoves) {
                    throw IllegalStateException("Selected token cannot move $dice steps.")
                }

                val outcome = LudoEngine.applyMove(
                    activeTokens = activeTokens,
                    opponentTokens = opponentTokens,
                    tokenIndex = tokenIndex,
                    dice = dice,
                    activeSlot = activeSlot,
                    activeName = callerIgn
                )

                val nextHost = (if (activeSlot == 0) outcome.nextActiveTokens else outcome.nextOpponentTokens).map { it.toLong() }
                val nextGuest = (if (activeSlot == 1) outcome.nextActiveTokens else outcome.nextOpponentTokens).map { it.toLong() }

                if (outcome.didWin) {
                    val userRef = db.collection("users").document(uid)
                    if (room.prizePool > 0L) {
                        val userSnap = tx.get(userRef)
                        val curWallet = userSnap.getLong("wallet") ?: 0L
                        val curWinning = userSnap.getLong("winningBalance") ?: 0L
                        tx.update(
                            userRef,
                            mapOf(
                                "wallet" to (curWallet + room.prizePool),
                                "winningBalance" to (curWinning + room.prizePool),
                                "lastLudoRoomId" to roomId,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    tx.update(
                        roomRef,
                        mapOf(
                            "status" to LudoRoomStatus.COMPLETED.wireValue,
                            "turnSlot" to activeSlot.toLong(),
                            "diceValue" to dice.toLong(),
                            "awaitingMove" to false,
                            "consecutiveSixes" to 0L,
                            "hostTokens" to nextHost,
                            "guestTokens" to nextGuest,
                            "lastAction" to if (room.prizePool > 0L) "$callerIgn won ₹${room.prizePool}!" else "$callerIgn won the Ludo match!",
                            "winnerUid" to uid,
                            "winnerIgn" to callerIgn.take(30),
                            "forfeitByUid" to "",
                            "prizePaid" to (room.prizePool > 0L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val nextSixes = if (outcome.extraTurn && dice == 6) room.consecutiveSixes else 0L
                    tx.update(
                        roomRef,
                        mapOf(
                            "turnSlot" to outcome.nextTurnSlot.toLong(),
                            "diceValue" to dice.toLong(),
                            "awaitingMove" to false,
                            "consecutiveSixes" to nextSixes,
                            "hostTokens" to nextHost,
                            "guestTokens" to nextGuest,
                            "lastAction" to outcome.summary.take(120),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun forfeitOnlineMatch(roomId: String): Result<Unit> {
        val path = "ludoRooms/$roomId"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("ludoRooms").document(roomId)

            db.runTransaction { tx ->
                val snap = tx.get(roomRef)
                val room = snap.toObject(LudoRoom::class.java) ?: throw IllegalStateException("Room not found.")
                if (room.status != LudoRoomStatus.PLAYING.wireValue) {
                    throw IllegalStateException("Match is already finished.")
                }
                if (uid != room.hostUid && uid != room.guestUid) {
                    throw IllegalStateException("Only match players can forfeit.")
                }

                val callerIsHost = uid == room.hostUid
                val callerIgn = if (callerIsHost) room.hostIgn else room.guestIgn
                val winnerUid = if (callerIsHost) room.guestUid else room.hostUid
                val winnerIgn = if (callerIsHost) room.guestIgn else room.hostIgn

                if (room.prizePool > 0L) {
                    val winnerRef = db.collection("users").document(winnerUid)
                    val winnerSnap = tx.get(winnerRef)
                    val curWallet = winnerSnap.getLong("wallet") ?: 0L
                    val curWinning = winnerSnap.getLong("winningBalance") ?: 0L
                    tx.update(
                        winnerRef,
                        mapOf(
                            "wallet" to (curWallet + room.prizePool),
                            "winningBalance" to (curWinning + room.prizePool),
                            "lastLudoRoomId" to roomId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.update(
                    roomRef,
                    mapOf(
                        "status" to LudoRoomStatus.COMPLETED.wireValue,
                        "awaitingMove" to false,
                        "lastAction" to "$callerIgn forfeited. $winnerIgn wins!",
                        "winnerUid" to winnerUid,
                        "winnerIgn" to winnerIgn.take(30),
                        "forfeitByUid" to uid,
                        "prizePaid" to (room.prizePool > 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Unit
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
