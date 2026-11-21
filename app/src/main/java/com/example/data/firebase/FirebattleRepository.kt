package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.ReferralRecord
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
                    val parsed = snapshot.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppSettings()
                    val resolvedUrl = parsed.appDownloadUrl.ifBlank { DEFAULT_APP_DOWNLOAD_URL }
                    val resolvedFee = if (snapshot.exists() && !snapshot.contains("withdrawalFeePercent")) {
                        0L
                    } else {
                        parsed.withdrawalFeePercent.coerceIn(0L, 20L)
                    }
                    val resolvedMinWd = if (snapshot.exists() && !snapshot.contains("minWithdrawal")) {
                        1L
                    } else {
                        parsed.minWithdrawal.coerceIn(1L, 5000L)
                    }
                    parsed.copy(
                        appDownloadUrl = resolvedUrl,
                        withdrawalFeePercent = resolvedFee,
                        minWithdrawal = resolvedMinWd
                    )
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

    fun observeMyReferrals(uid: String): Flow<List<ReferralRecord>> = flow {
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ReferralRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
        isExistingProfile: Boolean,
        optionalFriendReferralCode: String = ""
    ): Result<Unit> {
        val cleanIgn = ign.trim()
        val cleanGameUid = gameUid.trim()
        val cleanPhone = phone.trim()
        val cleanFriendCode = optionalFriendReferralCode.trim().uppercase()
        var path = "users"
        return try {
            val uid = requireUserId()
            require(cleanIgn.length in 2..30) { "IGN must be between 2 and 30 characters." }
            require(cleanGameUid.length in 3..30) { "Game UID must be between 3 and 30 characters." }
            require(PHONE_REGEX.matches(cleanPhone)) { "Enter a valid 10-digit mobile number." }
            if (!isExistingProfile && cleanFriendCode.isNotEmpty()) {
                require(REFERRAL_CODE_REGEX.matches(cleanFriendCode)) {
                    "Referral code must be 6 to 16 uppercase letters or digits."
                }
                val friendCodeSnap = db.collection("referralCodes").document(cleanFriendCode).get().await()
                if (!friendCodeSnap.exists()) {
                    throw IllegalArgumentException("Referral code '$cleanFriendCode' not found. Check it or leave empty.")
                }
                if (friendCodeSnap.getString("userId") == uid) {
                    throw IllegalArgumentException("You cannot apply your own referral code.")
                }
            }

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
                var created = false
                for (attempt in 0..4) {
                    val candidateCode = buildCandidateReferralCode(uid, attempt)
                    val codeRef = db.collection("referralCodes").document(candidateCode)
                    val committed = db.runTransaction { tx ->
                        val userSnap = tx.get(docRef)
                        val codeSnap = tx.get(codeRef)
                        if (userSnap.exists()) return@runTransaction true
                        if (codeSnap.exists()) return@runTransaction false

                        tx.set(
                            docRef,
                            mapOf(
                                "userId" to uid,
                                "email" to email,
                                "ign" to cleanIgn,
                                "gameUid" to cleanGameUid,
                                "phone" to cleanPhone,
                                "wallet" to 0L,
                                "referralCode" to candidateCode,
                                "referredByUid" to "",
                                "referredByCode" to "",
                                "referralCount" to 0L,
                                "referralEarnings" to 0L,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        tx.set(
                            codeRef,
                            mapOf(
                                "code" to candidateCode,
                                "userId" to uid,
                                "ign" to cleanIgn,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                        true
                    }.await()
                    if (committed) {
                        created = true
                        break
                    }
                }
                if (!created) throw IllegalStateException("Could not allocate a unique referral code. Please retry.")

                if (cleanFriendCode.isNotEmpty()) {
                    applyReferralCodeInternal(uid, cleanIgn, cleanFriendCode)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isExistingProfile) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureReferralCode(profile: UserProfile): Result<String> {
        if (profile.referralCode.isNotBlank()) return Result.success(profile.referralCode)
        var path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user mismatch." }
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val cleanIgn = profile.ign.trim().ifBlank { "Player" }.take(30)

            for (attempt in 0..4) {
                val candidate = buildCandidateReferralCode(uid, attempt)
                val codeRef = db.collection("referralCodes").document(candidate)
                val assigned = db.runTransaction { tx ->
                    val userSnap = tx.get(userRef)
                    val codeSnap = tx.get(codeRef)
                    val existing = userSnap.getString("referralCode").orEmpty()
                    if (existing.isNotBlank()) return@runTransaction existing
                    if (codeSnap.exists()) return@runTransaction null

                    tx.set(
                        codeRef,
                        mapOf(
                            "code" to candidate,
                            "userId" to uid,
                            "ign" to cleanIgn,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.update(
                        userRef,
                        mapOf(
                            "referralCode" to candidate,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    candidate
                }.await()
                if (!assigned.isNullOrBlank()) {
                    return Result.success(assigned)
                }
            }
            Result.failure(IllegalStateException("Unable to generate referral code right now."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun applyReferralCodeAtomic(profile: UserProfile, rawCode: String): Result<Long> {
        val cleanCode = rawCode.trim().uppercase()
        val path = "referrals/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId) { "Signed-in user changed; refusing referral claim." }
            require(REFERRAL_CODE_REGEX.matches(cleanCode)) {
                "Enter a valid 6 to 16 character alphanumeric referral code."
            }
            val cleanIgn = profile.ign.trim().ifBlank { "Player" }.take(30)
            val bonus = applyReferralCodeInternal(uid, cleanIgn, cleanCode)
            Result.success(bonus)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun applyReferralCodeInternal(uid: String, cleanIgn: String, cleanCode: String): Long {
        return db.runTransaction { tx ->
            val settingsRef = db.collection("settings").document("app")
            val codeRef = db.collection("referralCodes").document(cleanCode)
            val referralRef = db.collection("referrals").document(uid)
            val selfRef = db.collection("users").document(uid)

            val settingsSnap = tx.get(settingsRef)
            val codeSnap = tx.get(codeRef)
            val existingReferralSnap = tx.get(referralRef)
            val selfSnap = tx.get(selfRef)

            if (!codeSnap.exists()) throw IllegalStateException("Referral code '$cleanCode' not found.")
            val referrerUid = codeSnap.getString("userId").orEmpty()
            if (referrerUid.isBlank()) throw IllegalStateException("Referral code is invalid.")
            if (referrerUid == uid) throw IllegalStateException("You cannot apply your own referral code.")
            if (existingReferralSnap.exists() || selfSnap.getString("referredByUid").orEmpty().isNotBlank()) {
                throw IllegalStateException("You have already claimed a referral reward.")
            }

            val referrerRef = db.collection("users").document(referrerUid)
            val referrerSnap = tx.get(referrerRef)
            if (!referrerSnap.exists()) throw IllegalStateException("Referrer account no longer exists.")

            val bonus = if (settingsSnap.exists()) (settingsSnap.getLong("referralBonus") ?: 10L) else 10L
            if (bonus <= 0L) throw IllegalStateException("Referral rewards are currently paused by the organizer.")

            val referrerWallet = referrerSnap.getLong("wallet") ?: 0L
            val referrerCount = referrerSnap.getLong("referralCount") ?: 0L
            val referrerEarnings = referrerSnap.getLong("referralEarnings") ?: 0L
            val selfWallet = selfSnap.getLong("wallet") ?: 0L

            tx.set(
                referralRef,
                mapOf(
                    "referredUid" to uid,
                    "referredIgn" to cleanIgn,
                    "referrerUid" to referrerUid,
                    "referrerCode" to cleanCode,
                    "bonusAmount" to bonus,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                referrerRef,
                mapOf(
                    "wallet" to (referrerWallet + bonus),
                    "referralCount" to (referrerCount + 1L),
                    "referralEarnings" to (referrerEarnings + bonus),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                selfRef,
                mapOf(
                    "wallet" to (selfWallet + bonus),
                    "referredByUid" to referrerUid,
                    "referredByCode" to cleanCode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            bonus
        }.await()
    }

    private fun buildCandidateReferralCode(uid: String, attempt: Int): String {
        val cleaned = uid.filter { it.isLetterOrDigit() }.uppercase()
        val base = ("FB" + cleaned.take(6)).padEnd(8, 'X').take(8)
        if (attempt == 0) return base
        val suffix = ((uid.hashCode() xor (attempt * 7919)) and 0xFFFF)
            .toString(36)
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .padStart(2, '0')
            .take(2)
        return (base.take(6) + suffix).take(8)
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
            val settingsRef = db.collection("settings").document("app")
            val wdRef = db.collection("withdrawals").document()
            path = wdRef.path

            db.runTransaction { tx ->
                val settingsSnap = tx.get(settingsRef)
                val userSnap = tx.get(userRef)

                val minWithdrawal = if (settingsSnap.exists()) {
                    (settingsSnap.getLong("minWithdrawal") ?: 1L).coerceIn(1L, 5000L)
                } else {
                    1L
                }
                val feePercent = if (settingsSnap.exists()) {
                    (settingsSnap.getLong("withdrawalFeePercent") ?: 0L).coerceIn(0L, 20L)
                } else {
                    0L
                }

                if (amount < minWithdrawal) {
                    throw IllegalStateException("Minimum withdrawal is ₹$minWithdrawal.")
                }

                val currentWallet = userSnap.getLong("wallet") ?: 0L
                if (currentWallet < amount) {
                    throw IllegalStateException("Insufficient wallet balance (₹$currentWallet).")
                }

                val feeAmount = (amount * feePercent) / 100L
                val netPayout = amount - feeAmount
                if (netPayout < 1L) {
                    throw IllegalStateException("Net payout after platform fee must be at least ₹1.")
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
                        "feeAmount" to feeAmount,
                        "netPayout" to netPayout,
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
                        "referralBonus" to 10L,
                        "appDownloadUrl" to DEFAULT_APP_DOWNLOAD_URL,
                        "withdrawalFeePercent" to 5L,
                        "minWithdrawal" to 50L,
                        "sponsorTitle" to "",
                        "sponsorSubtitle" to "",
                        "sponsorUrl" to "",
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

    suspend fun saveAppSettings(
        upiId: String,
        upiName: String,
        adminPhone: String,
        referralBonus: Long = 10L,
        appDownloadUrl: String = DEFAULT_APP_DOWNLOAD_URL,
        withdrawalFeePercent: Long = 5L,
        minWithdrawal: Long = 50L,
        sponsorTitle: String = "",
        sponsorSubtitle: String = "",
        sponsorUrl: String = ""
    ): Result<Unit> {
        val cleanUpiId = upiId.trim()
        val cleanUpiName = upiName.trim()
        val cleanPhone = adminPhone.trim()
        val cleanDownloadUrl = appDownloadUrl.trim().ifBlank { DEFAULT_APP_DOWNLOAD_URL }
        val cleanSponsorTitle = sponsorTitle.trim()
        val cleanSponsorSubtitle = sponsorSubtitle.trim()
        val cleanSponsorUrl = sponsorUrl.trim()
        val path = "settings/app"
        return try {
            requireUserId()
            require(cleanUpiId.length in 3..80) { "UPI ID must be 3 to 80 characters." }
            require(cleanUpiName.length in 2..60) { "Receiver Name must be 2 to 60 characters." }
            require(PHONE_REGEX.matches(cleanPhone)) { "Admin phone must be 10 digits." }
            require(referralBonus in 0L..1000L) { "Referral bonus must be ₹0 to ₹1,000." }
            require(
                cleanDownloadUrl.length <= 300 &&
                    (cleanDownloadUrl.startsWith("https://") || cleanDownloadUrl.startsWith("http://"))
            ) {
                "App Download Link must start with https:// or http:// (max 300 chars)."
            }
            require(withdrawalFeePercent in 0L..20L) { "Platform withdrawal fee must be 0% to 20%." }
            require(minWithdrawal in 1L..5000L) { "Minimum withdrawal must be ₹1 to ₹5,000." }
            require(cleanSponsorTitle.length <= 100) { "Sponsor headline max 100 characters." }
            require(cleanSponsorSubtitle.length <= 180) { "Sponsor subtitle max 180 characters." }
            require(
                cleanSponsorUrl.isEmpty() ||
                    (cleanSponsorUrl.length <= 300 &&
                        (cleanSponsorUrl.startsWith("https://") || cleanSponsorUrl.startsWith("http://")))
            ) {
                "Sponsor link must start with https:// or http:// (max 300 chars)."
            }

            db.collection("settings").document("app").set(
                mapOf(
                    "upiId" to cleanUpiId,
                    "upiName" to cleanUpiName,
                    "adminPhone" to cleanPhone,
                    "referralBonus" to referralBonus,
                    "appDownloadUrl" to cleanDownloadUrl,
                    "withdrawalFeePercent" to withdrawalFeePercent,
                    "minWithdrawal" to minWithdrawal,
                    "sponsorTitle" to cleanSponsorTitle,
                    "sponsorSubtitle" to cleanSponsorSubtitle,
                    "sponsorUrl" to cleanSponsorUrl,
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
        private val REFERRAL_CODE_REGEX = Regex("^[A-Z0-9]{6,16}$")
        const val BOOTSTRAPPED_ADMIN_EMAIL = "chauhanjignesh8320@gmail.com"
    }
}
