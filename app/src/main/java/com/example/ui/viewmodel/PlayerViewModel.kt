package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebattleRepository
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.ReferralRecord
import com.example.data.model.TournamentMatch
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WithdrawalRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
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

    fun joinMatch(match: TournamentMatch, profile: UserProfile) {
        runAction("Successfully joined ${match.title}!") {
            repository.joinMatchAtomic(match.id, profile)
        }
    }

    fun submitDeposit(profile: UserProfile, amountText: String, utr: String, onSuccess: () -> Unit = {}) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount <= 0L) {
            _events.tryEmit("Enter a valid deposit amount.")
            return
        }
        runAction("Deposit request submitted for admin approval.") {
            repository.submitDepositRequest(profile, amount, utr).also {
                if (it.isSuccess) onSuccess()
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
