package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class LudoRoomStatus(val wireValue: String) {
    WAITING("WAITING"),
    PLAYING("PLAYING"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    companion object {
        fun fromRaw(raw: String?): LudoRoomStatus =
            entries.firstOrNull { it.wireValue == raw || it.name == raw } ?: WAITING
    }
}

enum class LudoPlayMode {
    LOBBY,
    VS_BOT,
    PASS_AND_PLAY,
    ONLINE_ROOM
}

@Keep
@IgnoreExtraProperties
data class LudoRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val hostUid: String = "",
    val hostIgn: String = "",
    val guestUid: String = "",
    val guestIgn: String = "",
    val entry: Long = 0L,
    val prizePool: Long = 0L,
    val status: String = LudoRoomStatus.WAITING.wireValue,
    val turnSlot: Long = 0L,
    val diceValue: Long = 0L,
    val awaitingMove: Boolean = false,
    val consecutiveSixes: Long = 0L,
    val hostTokens: List<Long> = listOf(0L, 0L, 0L, 0L),
    val guestTokens: List<Long> = listOf(0L, 0L, 0L, 0L),
    val lastAction: String = "",
    val winnerUid: String = "",
    val winnerIgn: String = "",
    val forfeitByUid: String = "",
    val prizePaid: Boolean = false,
    val matchId: String = "",
    val boardNumber: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isTournamentBoard: Boolean
        get() = matchId.isNotBlank() && boardNumber > 0L

    companion object {
        fun calculatePrizePool(entry: Long): Long =
            if (entry <= 0L) 0L else (entry * 2L * 90L) / 100L
    }
}

object TournamentLudoPairing {
    fun teamHalf(totalSlots: Long): Long = (totalSlots / 2L).coerceAtLeast(1L)

    fun boardCount(totalSlots: Long): Int = teamHalf(totalSlots).toInt().coerceIn(1, 50)

    fun boardForSlot(slotNumber: Long, totalSlots: Long): Long {
        val half = teamHalf(totalSlots)
        val safeSlot = slotNumber.coerceIn(1L, half * 2L)
        return if (safeSlot <= half) safeSlot else safeSlot - half
    }

    fun opponentSlotFor(slotNumber: Long, totalSlots: Long): Long {
        val half = teamHalf(totalSlots)
        val safeSlot = slotNumber.coerceIn(1L, half * 2L)
        return if (safeSlot <= half) safeSlot + half else safeSlot - half
    }

    fun roomDocId(matchId: String, boardNumber: Long): String =
        "tm_${matchId}_b$boardNumber"
}
