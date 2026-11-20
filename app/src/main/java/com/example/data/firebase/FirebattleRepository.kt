package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.TournamentMatch
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
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
import kotlin.math.max

class FirebattleRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
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

    fun observeIsAdmin(uid: String): Flow<Boolean> = flow {
        val user = auth.currentUser
        val isBootstrappedOwner = user != null &&
            user.uid == uid &&
            user.isEmailVerified &&
            user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
        if (isBootstrappedOwner) {
            emit(true)
            return@flow
        }
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snapshot -> snapshot.exists() }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError, OperationType.GET, path)
                    }
                    emit(false)
                }
        )
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeAppSettings(uid: String): Flow<AppSettings> = flow {
        val path = "settings/app"
        emitAll(
            db.collection("settings").document("app")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppSettings()
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeLiveMatches(uid: String): Flow<List<TournamentMatch>> = flow {
        val path = "matches"
        emitAll(
            db.collection("matches")
                .whereEqualTo("status", MatchStatus.LIVE.wireValue)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TournamentMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllMatches(uid: String): Flow<List<TournamentMatch>> = flow {
        val path = "matches"
        emitAll(
            db.collection("matches")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TournamentMatch::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeMyRegistrations(uid: String): Flow<List<MatchRegistration>> = flow {
        val path = "registrations"
        emitAll(
            db.collection("registrations")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MatchRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllRegistrations(uid: String): Flow<List<MatchRegistration>> = flow {
        val path = "registrations"
        emitAll(
            db.collection("registrations")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MatchRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeMyDeposits(uid: String): Flow<List<DepositRequest>> = flow {
        val path = "deposits"
        emitAll(
            db.collection("deposits")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllDeposits(uid: String): Flow<List<DepositRequest>> = flow {
        val path = "deposits"
        emitAll(
            db.collection("deposits")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeMyWithdrawals(uid: String): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection("withdrawals")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllWithdrawals(uid: String): Flow<List<WithdrawalRequest>> = flow {
        val path = "withdrawals"
        emitAll(
            db.collection("withdrawals")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeAllUsers(uid: String): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    suspend fun savePlayerProfile(
        ign: String,
        gameUid: String,
        phone: String,
        isExistingProfile: Boolean
    ): Result<Unit> {
        val cleanIgn = ign.trim()
        val cleanGameUid = gameUid.trim()
        val cleanPhone = phone.trim()
        var path = "users"
        return try {
            val uid = requireUserId()
            require(cleanIgn.length in 2..30) { "IGN must be between 2 and 30 characters." }
            require(cleanGameUid.length in 3..30) { "Game UID must be between 3 and 30 characters." }
            require(PHONE_REGEX.matches(cleanPhone)) { "Enter a valid 10-digit mobile number." }

            val docRef = db.collection("users").document(uid)
            path = docRef.path
            if (isExistingProfile) {
                docRef.update(
                    mapOf(
                        "ign" to cleanIgn,
                        "gameUid" to cleanGameUid,
                        "phone" to cleanPhone,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val email = (auth.currentUser?.email ?: "").take(120)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "email" to email,
                        "ign" to cleanIgn,
                        "gameUid" to cleanGameUid,
                        "phone" to cleanPhone,
                        "wallet" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isExistingProfile) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinMatchAtomic(matchId: String, profile: UserProfile): Result<Unit> {
        var path = "matches/$matchId"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed; refusing match join." }
            require(matchId.isNotBlank()) { "Match ID is invalid." }

            val matchRef = db.collection("matches").document(matchId)
            val userRef = db.collection("users").document(uid)
            val regId = "${matchId}_$uid"
            val regRef = db.collection("registrations").document(regId)
            path = regRef.path

            db.runTransaction { tx ->
                val matchSnap = tx.get(matchRef)
                val userSnap = tx.get(userRef)
                val existingReg = tx.get(regRef)

                if (!matchSnap.exists()) throw IllegalStateException("Tournament not found.")
                if (existingReg.exists()) throw IllegalStateException("You have already joined this match.")

                val status = matchSnap.getString("status") ?: ""
                if (status != MatchStatus.LIVE.wireValue) throw IllegalStateException("Match is no longer open.")

                val joinedCount = matchSnap.getLong("joinedCount") ?: 0L
                val slots = matchSnap.getLong("slots") ?: 0L
                if (joinedCount >= slots) throw IllegalStateException("All slots are full.")

                val entry = matchSnap.getLong("entry") ?: 0L
                val currentWallet = userSnap.getLong("wallet") ?: 0L
                if (currentWallet < entry) {
                    throw IllegalStateException("Insufficient wallet balance (₹$currentWallet). Deposit ₹${entry - currentWallet} more.")
                }

                tx.set(
                    regRef,
                    mapOf(
                        "matchId" to matchId,
                        "userId" to uid,
                        "ign" to profile.ign,
                        "gameUid" to profile.gameUid,
                        "phone" to profile.phone,
                        "entryPaid" to entry,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    matchRef,
                    mapOf(
                        "joinedCount" to (joinedCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (entry > 0L) {
                    tx.update(
                        userRef,
                        mapOf(
                            "wallet" to (currentWallet - entry),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun submitDepositRequest(profile: UserProfile, amount: Long, utr: String): Result<Unit> {
        val cleanUtr = utr.trim()
        var path = "deposits"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed; refusing deposit request." }
            require(amount in 1L..100_000L) { "Amount must be between ₹1 and ₹1,00,000." }
            require(cleanUtr.length in 6..32) { "Enter a valid 6 to 32 character UTR / reference number." }

            val docRef = db.collection("deposits").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "phone" to profile.phone,
                    "ign" to profile.ign,
                    "amount" to amount,
                    "utr" to cleanUtr,
                    "status" to "Pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalAtomic(profile: UserProfile, amount: Long, upiId: String): Result<Unit> {
        val cleanUpi = upiId.trim()
        var path = "withdrawals"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed; refusing withdrawal request." }
            require(amount in 1L..100_000L) { "Withdraw amount must be between ₹1 and ₹1,00,000." }
            require(cleanUpi.length in 3..80 && cleanUpi.contains("@")) { "Enter a valid UPI ID (e.g. player@ybl)." }

            val userRef = db.collection("users").document(uid)
            val wdRef = db.collection("withdrawals").document()
            path = wdRef.path

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val currentWallet = userSnap.getLong("wallet") ?: 0L
                if (currentWallet < amount) {
                    throw IllegalStateException("Insufficient wallet balance (₹$currentWallet).")
                }
                tx.update(
                    userRef,
                    mapOf(
                        "wallet" to (currentWallet - amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    wdRef,
                    mapOf(
                        "userId" to uid,
                        "phone" to profile.phone,
                        "ign" to profile.ign,
                        "amount" to amount,
                        "upiId" to cleanUpi,
                        "status" to "Pending",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    suspend fun ensureDefaultSettingsIfMissing(): Result<Unit> {
        val path = "settings/app"
        return try {
            requireUserId()
            val ref = db.collection("settings").document("app")
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "upiId" to "firebattle@upi",
                        "upiName" to "Firebattle Esports",
                        "adminPhone" to "9876543210",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createMatch(
        title: String,
        game: String,
        matchTime: String,
        mapName: String,
        entry: Long,
        perKill: Long,
        prize: Long,
        slots: Long
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanTime = matchTime.trim()
        val cleanMap = mapName.trim().ifEmpty { "Standard" }
        var path = "matches"
        return try {
            val uid = requireUserId()
            require(cleanTitle.length in 2..80) { "Title must be 2 to 80 characters." }
            require(game == "Free Fire MAX" || game == "BGMI") { "Select Free Fire MAX or BGMI." }
            require(cleanTime.length in 2..50) { "Match time must be 2 to 50 characters." }
            require(cleanMap.length in 2..40) { "Map name must be 2 to 40 characters." }
            require(entry in 0L..100_000L) { "Entry fee must be ₹0 to ₹1,00,000." }
            require(perKill in 0L..100_000L) { "Per kill reward must be ₹0 to ₹1,00,000." }
            require(prize in 0L..10_000_000L) { "Prize pool must be non-negative." }
            require(slots in 2L..200L) { "Slots must be between 2 and 200." }

            val docRef = db.collection("matches").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "game" to game,
                    "matchTime" to cleanTime,
                    "mapName" to cleanMap,
                    "entry" to entry,
                    "perKill" to perKill,
                    "prize" to prize,
                    "slots" to slots,
                    "joinedCount" to 0L,
                    "roomId" to "",
                    "roomPass" to "",
                    "status" to MatchStatus.LIVE.wireValue,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRoomCredentials(matchId: String, roomId: String, roomPass: String): Result<Unit> {
        val cleanId = roomId.trim()
        val cleanPass = roomPass.trim()
        val path = "matches/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank()) { "Match ID missing." }
            require(cleanId.length <= 40 && cleanPass.length <= 40) { "Room credentials must be at most 40 characters." }
            db.collection("matches").document(matchId).update(
                mapOf(
                    "roomId" to cleanId,
                    "roomPass" to cleanPass,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMatchStatus(matchId: String, newStatus: MatchStatus): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank()) { "Match ID missing." }
            db.collection("matches").document(matchId).update(
                mapOf(
                    "status" to newStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMatch(matchId: String): Result<Unit> {
        val path = "matches/$matchId"
        return try {
            requireUserId()
            require(matchId.isNotBlank()) { "Match ID missing." }
            db.collection("matches").document(matchId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun approveDepositAtomic(deposit: DepositRequest): Result<Unit> {
        val path = "deposits/${deposit.id}"
        return try {
            requireUserId()
            require(deposit.id.isNotBlank() && deposit.userId.isNotBlank()) { "Invalid deposit record." }

            val depRef = db.collection("deposits").document(deposit.id)
            val userRef = db.collection("users").document(deposit.userId)
            db.runTransaction { tx ->
                val depSnap = tx.get(depRef)
                val userSnap = tx.get(userRef)
                if (!depSnap.exists()) throw IllegalStateException("Deposit request not found.")
                if (depSnap.getString("status") != "Pending") throw IllegalStateException("Deposit already processed.")
                val amount = depSnap.getLong("amount") ?: 0L
                val currentWallet = userSnap.getLong("wallet") ?: 0L

                tx.update(
                    depRef,
                    mapOf(
                        "status" to "Approved",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "wallet" to (currentWallet + amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectDeposit(depositId: String): Result<Unit> {
        val path = "deposits/$depositId"
        return try {
            requireUserId()
            require(depositId.isNotBlank()) { "Deposit ID missing." }
            db.collection("deposits").document(depositId).update(
                mapOf(
                    "status" to "Rejected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markWithdrawalPaid(withdrawalId: String): Result<Unit> {
        val path = "withdrawals/$withdrawalId"
        return try {
            requireUserId()
            require(withdrawalId.isNotBlank()) { "Withdrawal ID missing." }
            db.collection("withdrawals").document(withdrawalId).update(
                mapOf(
                    "status" to "Paid",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectAndRefundWithdrawalAtomic(withdrawal: WithdrawalRequest): Result<Unit> {
        val path = "withdrawals/${withdrawal.id}"
        return try {
            requireUserId()
            require(withdrawal.id.isNotBlank() && withdrawal.userId.isNotBlank()) { "Invalid withdrawal record." }

            val wdRef = db.collection("withdrawals").document(withdrawal.id)
            val userRef = db.collection("users").document(withdrawal.userId)
            db.runTransaction { tx ->
                val wdSnap = tx.get(wdRef)
                val userSnap = tx.get(userRef)
                if (!wdSnap.exists()) throw IllegalStateException("Withdrawal request not found.")
                if (wdSnap.getString("status") != "Pending") throw IllegalStateException("Withdrawal already processed.")
                val amount = wdSnap.getLong("amount") ?: 0L
                val currentWallet = userSnap.getLong("wallet") ?: 0L

                tx.update(
                    wdRef,
                    mapOf(
                        "status" to "Rejected",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "wallet" to (currentWallet + amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustUserWalletAtomic(targetUserId: String, delta: Long): Result<Unit> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "Target user ID missing." }
            require(delta != 0L) { "Enter a non-zero amount." }

            val userRef = db.collection("users").document(targetUserId)
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("Player profile not found.")
                val current = userSnap.getLong("wallet") ?: 0L
                val updated = max(0L, current + delta).coerceAtMost(10_000_000L)
                tx.update(
                    userRef,
                    mapOf(
                        "wallet" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAppSettings(upiId: String, upiName: String, adminPhone: String): Result<Unit> {
        val cleanUpiId = upiId.trim()
        val cleanUpiName = upiName.trim()
        val cleanPhone = adminPhone.trim()
        val path = "settings/app"
        return try {
            requireUserId()
            require(cleanUpiId.length in 3..80) { "UPI ID must be 3 to 80 characters." }
            require(cleanUpiName.length in 2..60) { "Receiver Name must be 2 to 60 characters." }
            require(PHONE_REGEX.matches(cleanPhone)) { "Admin phone must be 10 digits." }

            db.collection("settings").document("app").set(
                mapOf(
                    "upiId" to cleanUpiId,
                    "upiName" to cleanUpiName,
                    "adminPhone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        private val PHONE_REGEX = Regex("^[0-9]{10}$")
        const val BOOTSTRAPPED_ADMIN_EMAIL = "chauhanjignesh8320@gmail.com"
    }
}
