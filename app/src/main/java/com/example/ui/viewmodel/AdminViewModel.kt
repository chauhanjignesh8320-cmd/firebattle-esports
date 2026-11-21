package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebattleRepository
import com.example.data.model.AppSettings
import com.example.data.model.DepositRequest
import com.example.data.model.MatchRegistration
import com.example.data.model.MatchStatus
import com.example.data.model.OwnerPayout
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

data class AdminSettingsDraft(
    val upiId: String,
    val upiName: String,
    val adminPhone: String,
    val referralBonusText: String,
    val appDownloadUrl: String,
    val withdrawalFeePercentText: String,
    val minWithdrawalText: String,
    val sponsorTitle: String,
    val sponsorSubtitle: String,
    val sponsorUrl: String
)

class AdminViewModel(
    private val repository: FirebattleRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettingsIfMissing()
        }
    }

    val allMatchesState: StateFlow<UiState<List<TournamentMatch>>> = repository.observeAllMatches(currentUserId)
        .toUiState("admin matches")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val registrationsByMatchState: StateFlow<UiState<Map<String, List<MatchRegistration>>>> =
        repository.observeAllRegistrations(currentUserId)
            .map { regs -> regs.groupBy { it.matchId } }
            .toUiState("registrations")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val allDepositsState: StateFlow<UiState<List<DepositRequest>>> = repository.observeAllDeposits(currentUserId)
        .toUiState("deposits")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val allWithdrawalsState: StateFlow<UiState<List<WithdrawalRequest>>> = repository.observeAllWithdrawals(currentUserId)
        .toUiState("withdrawals")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val allUsersState: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsers(currentUserId)
        .toUiState("players")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val settingsState: StateFlow<UiState<AppSettings>> = repository.observeAppSettings(currentUserId)
        .toUiState("settings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val ownerPayoutsState: StateFlow<UiState<List<OwnerPayout>>> = repository.observeOwnerPayouts(currentUserId)
        .toUiState("owner payouts")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    fun createMatch(
        title: String,
        game: String,
        matchTime: String,
        mapName: String,
        entryText: String,
        perKillText: String,
        prizeText: String,
        slotsText: String,
        onSuccess: () -> Unit
    ) {
        val entry = entryText.trim().ifEmpty { "0" }.toLongOrNull()
        val perKill = perKillText.trim().ifEmpty { "0" }.toLongOrNull()
        val prize = prizeText.trim().ifEmpty { "0" }.toLongOrNull()
        val slots = slotsText.trim().toLongOrNull()

        if (entry == null || perKill == null || prize == null || slots == null) {
            _events.tryEmit("Enter valid numeric values for fees, prize, and slots.")
            return
        }

        runAction("Tournament published live!") {
            repository.createMatch(title, game, matchTime, mapName, entry, perKill, prize, slots).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    fun updateRoomCredentials(matchId: String, roomId: String, roomPass: String) {
        runAction("Room ID & Password updated!") {
            repository.updateRoomCredentials(matchId, roomId, roomPass)
        }
    }

    fun toggleMatchStatus(match: TournamentMatch) {
        val next = if (MatchStatus.fromRaw(match.status) == MatchStatus.LIVE) {
            MatchStatus.COMPLETED
        } else {
            MatchStatus.LIVE
        }
        runAction("Match marked ${next.wireValue}.") {
            repository.updateMatchStatus(match.id, next)
        }
    }

    fun deleteMatch(matchId: String) {
        runAction("Tournament deleted.") {
            repository.deleteMatch(matchId)
        }
    }

    fun approveDeposit(deposit: DepositRequest) {
        runAction("₹${deposit.amount} credited to ${deposit.ign}!") {
            repository.approveDepositAtomic(deposit)
        }
    }

    fun rejectDeposit(depositId: String) {
        runAction("Deposit request rejected.") {
            repository.rejectDeposit(depositId)
        }
    }

    fun markWithdrawalPaid(withdrawalId: String) {
        runAction("Withdrawal marked as Paid!") {
            repository.markWithdrawalPaid(withdrawalId)
        }
    }

    fun rejectAndRefundWithdrawal(withdrawal: WithdrawalRequest) {
        runAction("₹${withdrawal.amount} refunded to ${withdrawal.ign}!") {
            repository.rejectAndRefundWithdrawalAtomic(withdrawal)
        }
    }

    fun adjustUserWallet(user: UserProfile, deltaText: String, onSuccess: () -> Unit) {
        val delta = deltaText.trim().toLongOrNull()
        if (delta == null || delta == 0L) {
            _events.tryEmit("Enter a valid non-zero amount (e.g. 50 or -50).")
            return
        }
        runAction("Updated ${user.ign}'s wallet!") {
            repository.adjustUserWalletAtomic(user.userId, delta).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    fun withdrawOwnerProfit(
        amountText: String,
        upiId: String,
        note: String,
        availableProfit: Long,
        onSuccess: () -> Unit
    ) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount <= 0L) {
            _events.tryEmit("Enter a valid positive profit withdrawal amount.")
            return
        }
        if (availableProfit > 0L && amount > availableProfit) {
            _events.tryEmit("Available organizer profit is ₹$availableProfit.")
            return
        }
        runAction("₹$amount profit withdrawal recorded!") {
            repository.recordOwnerProfitWithdrawal(amount, upiId, note).also {
                if (it.isSuccess) onSuccess()
            }
        }
    }

    fun saveSettings(draft: AdminSettingsDraft) {
        val bonus = draft.referralBonusText.trim().ifEmpty { "0" }.toLongOrNull()
        val feePercent = draft.withdrawalFeePercentText.trim().ifEmpty { "0" }.toLongOrNull()
        val minWd = draft.minWithdrawalText.trim().ifEmpty { "50" }.toLongOrNull()
        if (bonus == null) {
            _events.tryEmit("Enter a valid numeric referral bonus (₹0 - ₹1000).")
            return
        }
        if (feePercent == null || feePercent !in 0L..20L) {
            _events.tryEmit("Platform withdrawal fee must be between 0% and 20%.")
            return
        }
        if (minWd == null || minWd !in 1L..5000L) {
            _events.tryEmit("Minimum withdrawal must be between ₹1 and ₹5,000.")
            return
        }
        runAction("Settings saved to Firestore!") {
            repository.saveAppSettings(
                upiId = draft.upiId,
                upiName = draft.upiName,
                adminPhone = draft.adminPhone,
                referralBonus = bonus,
                appDownloadUrl = draft.appDownloadUrl,
                withdrawalFeePercent = feePercent,
                minWithdrawal = minWd,
                sponsorTitle = draft.sponsorTitle,
                sponsorSubtitle = draft.sponsorSubtitle,
                sponsorUrl = draft.sponsorUrl
            )
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
                    onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Admin action failed.") }
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
        const val TAG = "AdminVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
