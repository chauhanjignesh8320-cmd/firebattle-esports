package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdminPushNotifier
import com.example.data.firebase.FirebattleRepository
import com.example.data.firebase.PhonePeCreateOrderResult
import com.example.data.firebase.PhonePeGatewayClient
import com.example.data.firebase.PhonePeVerifyResult
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.ReferralRecord
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlayerViewModel(
    private val repository: FirebattleRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .toUiState("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val settingsState: StateFlow<UiState<AppSettings>> = repository.observeAppSettings(currentUserId)
        .toUiState("settings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val liveMatchesState: StateFlow<UiState<List<TournamentMatch>>> = repository.observeLiveMatches(currentUserId)
        .toUiState("matches")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val completedMatchesState: StateFlow<UiState<List<TournamentMatch>>> = repository.observeCompletedMatches(currentUserId)
        .toUiState("completed matches")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myRegistrationsListState: StateFlow<UiState<List<MatchRegistration>>> = repository.observeMyRegistrations(currentUserId)
        .toUiState("registration details")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myRegistrationsState: StateFlow<UiState<Set<String>>> = myRegistrationsListState
        .map { state ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(state.message)
                is UiState.Success -> UiState.Success(state.data.map { it.matchId }.toSet())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myDepositsState: StateFlow<UiState<List<DepositRequest>>> = repository.observeMyDeposits(currentUserId)
        .toUiState("deposits")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myWithdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> = repository.observeMyWithdrawals(currentUserId)
        .toUiState("withdrawals")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myReferralsState: StateFlow<UiState<List<ReferralRecord>>> = repository.observeMyReferrals(currentUserId)
        .toUiState("referrals")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private var referralCodeEnsured = false
    private var installHeartbeatRecorded = false

    fun recordDeviceInstallHeartbeat(installId: String, deviceModel: String, appVersion: String) {
        if (installHeartbeatRecorded) return
        installHeartbeatRecorded = true
        viewModelScope.launch {
            val result = repository.recordDeviceInstallHeartbeat(installId, deviceModel, appVersion)
            if (result.isFailure) {
                installHeartbeatRecorded = false
            }
        }
    }

    fun ensureReferralCodeIfMissing(profile: UserProfile) {
        if (profile.referralCode.isNotBlank() || referralCodeEnsured) return
        referralCodeEnsured = true
        viewModelScope.launch {
            val result = repository.ensureReferralCode(profile)
            if (result.isFailure) {
                referralCodeEnsured = false
            }
        }
    }

    fun saveProfile(
        ign: String,
        gameUid: String,
        phone: String,
        isExisting: Boolean,
        optionalFriendReferralCode: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val msg = if (!isExisting && optionalFriendReferralCode.isNotBlank()) {
            "Profile saved & referral bonus credited!"
        } else {
            "Profile saved!"
        }
        runAction(msg) {
            repository.savePlayerProfile(ign, gameUid, phone, isExisting, optionalFriendReferralCode).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    fun applyReferralCode(profile: UserProfile, rawCode: String, onSuccess: () -> Unit = {}) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = repository.applyReferralCodeAtomic(profile, rawCode)
                result.fold(
                    onSuccess = { bonus ->
                        _events.tryEmit("Referral applied! ₹$bonus added to your wallet & your friend's wallet.")
                        onSuccess()
                    },
                    onFailure = { err ->
                        _events.tryEmit(err.localizedMessage ?: "Failed to apply referral code.")
                    }
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun joinMatch(
        match: TournamentMatch,
        profile: UserProfile,
        slotNumber: Long,
        customIgn: String
    ) {
        val cleanIgn = customIgn.trim().ifBlank { profile.ign }
        runAction("Joined ${match.title} • Slot #$slotNumber ($cleanIgn)!") {
            repository.joinMatchAtomic(match.id, profile, slotNumber, cleanIgn)
        }
    }

    fun observeAdminDepositsFlow(): Flow<List<DepositRequest>> =
        repository.observeAllDeposits(currentUserId)

    fun submitDeposit(profile: UserProfile, amountText: String, utr: String, onSuccess: () -> Unit = {}) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount <= 0L) {
            _events.tryEmit("Enter a valid deposit amount.")
            return
        }
        runAction("₹$amount deposit request sent to Admin! Wallet updates once verified.") {
            repository.submitDepositRequest(profile, amount, utr).map { depositId ->
                onSuccess()
                viewModelScope.launch {
                    val idToken = runCatching {
                        Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                    }.getOrNull()
                    if (!idToken.isNullOrBlank()) {
                        AdminPushNotifier.notifyNewDeposit(idToken, depositId)
                    }
                }
            }
        }
    }

    fun startPhonePeCheckout(
        amount: Long,
        onCheckoutReady: (merchantOrderId: String, redirectUrl: String) -> Unit,
        onFallbackDirectUpi: () -> Unit
    ) {
        if (amount !in 1L..100_000L) {
            _events.tryEmit("Enter an amount between ₹1 and ₹1,00,000.")
            return
        }
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                if (idToken.isNullOrBlank()) {
                    _events.tryEmit("Please sign in again to add money.")
                    return@launch
                }
                when (val result = PhonePeGatewayClient.createOrder(idToken, amount)) {
                    is PhonePeCreateOrderResult.Success -> {
                        onCheckoutReady(result.merchantOrderId, result.redirectUrl)
                    }
                    is PhonePeCreateOrderResult.NotConfigured -> {
                        onFallbackDirectUpi()
                    }
                    is PhonePeCreateOrderResult.Error -> {
                        _events.tryEmit(result.message)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _events.tryEmit(e.localizedMessage ?: "Failed to start payment.")
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun verifyPhonePeOrder(orderId: String, onDone: (state: String, summary: String) -> Unit = { _, _ -> }) {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) return
        viewModelScope.launch {
            try {
                val idToken = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: return@launch
                var result = PhonePeGatewayClient.verifyOrder(idToken, cleanOrderId)
                if (result is PhonePeVerifyResult.Pending) {
                    delay(1500L)
                    result = PhonePeGatewayClient.verifyOrder(idToken, cleanOrderId)
                }
                when (result) {
                    is PhonePeVerifyResult.Completed -> {
                        val total = result.amount + result.bonusAmount
                        val msg = if (result.bonusAmount > 0L) {
                            "₹${result.amount} + ₹${result.bonusAmount} bonus (₹$total) added to your wallet!"
                        } else {
                            "₹${result.amount} added to your wallet via PhonePe PG!"
                        }
                        _events.tryEmit(msg)
                        onDone("COMPLETED", msg)
                    }
                    is PhonePeVerifyResult.Failed -> {
                        _events.tryEmit(result.message)
                        onDone("FAILED", result.message)
                    }
                    is PhonePeVerifyResult.Pending -> {
                        val msg = "Payment is still processing. Your wallet updates automatically once confirmed."
                        _events.tryEmit(msg)
                        onDone("PENDING", msg)
                    }
                    is PhonePeVerifyResult.Error -> {
                        _events.tryEmit(result.message)
                        onDone("ERROR", result.message)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _events.tryEmit(e.localizedMessage ?: "Unable to verify PhonePe payment.")
            }
        }
    }

    fun submitWithdrawal(profile: UserProfile, amountText: String, upiId: String, onSuccess: () -> Unit = {}) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount <= 0L) {
            _events.tryEmit("Enter a valid withdrawal amount.")
            return
        }
        runAction("Withdrawal request submitted!") {
            repository.submitWithdrawalAtomic(profile, amount, upiId).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = block()
                result.fold(
                    onSuccess = { _events.tryEmit(successMessage) },
                    onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Action failed.") }
                )
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }

    private companion object {
        const val TAG = "PlayerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
