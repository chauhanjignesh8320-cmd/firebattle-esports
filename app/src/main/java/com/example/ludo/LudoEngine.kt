package com.example.ludo

import kotlin.math.abs

data class BoardCell(val row: Int, val col: Int)

data class LudoMoveOutcome(
    val nextActiveTokens: List<Int>,
    val nextOpponentTokens: List<Int>,
    val capturedOpponent: Boolean,
    val reachedHome: Boolean,
    val extraTurn: Boolean,
    val didWin: Boolean,
    val nextTurnSlot: Int,
    val nextConsecutiveSixes: Int,
    val summary: String
)

object LudoEngine {
    const val HOME_PROGRESS = 57
    const val HOST_START_INDEX = 0
    const val GUEST_START_INDEX = 26

    val SAFE_TRACK_INDICES: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // 52-cell clockwise outer track on 15x15 Ludo board
    val TRACK_COORDS: List<BoardCell> = listOf(
        // 0..4 (Red arm top row, left->right)
        BoardCell(6, 1), BoardCell(6, 2), BoardCell(6, 3), BoardCell(6, 4), BoardCell(6, 5),
        // 5..10 (Top arm left col, bottom->top)
        BoardCell(5, 6), BoardCell(4, 6), BoardCell(3, 6), BoardCell(2, 6), BoardCell(1, 6), BoardCell(0, 6),
        // 11 (Top middle)
        BoardCell(0, 7),
        // 12..17 (Top arm right col, top->bottom) - 13 is Green start
        BoardCell(0, 8), BoardCell(1, 8), BoardCell(2, 8), BoardCell(3, 8), BoardCell(4, 8), BoardCell(5, 8),
        // 18..23 (Right arm top row, left->right)
        BoardCell(6, 9), BoardCell(6, 10), BoardCell(6, 11), BoardCell(6, 12), BoardCell(6, 13), BoardCell(6, 14),
        // 24 (Right middle)
        BoardCell(7, 14),
        // 25..30 (Right arm bottom row, right->left) - 26 is Yellow start (Guest)
        BoardCell(8, 14), BoardCell(8, 13), BoardCell(8, 12), BoardCell(8, 11), BoardCell(8, 10), BoardCell(8, 9),
        // 31..36 (Bottom arm right col, top->bottom)
        BoardCell(9, 8), BoardCell(10, 8), BoardCell(11, 8), BoardCell(12, 8), BoardCell(13, 8), BoardCell(14, 8),
        // 37 (Bottom middle)
        BoardCell(14, 7),
        // 38..43 (Bottom arm left col, bottom->top) - 39 is Blue start
        BoardCell(14, 6), BoardCell(13, 6), BoardCell(12, 6), BoardCell(11, 6), BoardCell(10, 6), BoardCell(9, 6),
        // 44..49 (Left arm bottom row, right->left)
        BoardCell(8, 5), BoardCell(8, 4), BoardCell(8, 3), BoardCell(8, 2), BoardCell(8, 1), BoardCell(8, 0),
        // 50 (Left middle)
        BoardCell(7, 0),
        // 51 (Wrap before Red start)
        BoardCell(6, 0)
    )

    val RED_HOME_STRETCH: List<BoardCell> = listOf(
        BoardCell(7, 1), BoardCell(7, 2), BoardCell(7, 3), BoardCell(7, 4), BoardCell(7, 5)
    )

    val YELLOW_HOME_STRETCH: List<BoardCell> = listOf(
        BoardCell(7, 13), BoardCell(7, 12), BoardCell(7, 11), BoardCell(7, 10), BoardCell(7, 9)
    )

    val GREEN_HOME_STRETCH: List<BoardCell> = listOf(
        BoardCell(1, 7), BoardCell(2, 7), BoardCell(3, 7), BoardCell(4, 7), BoardCell(5, 7)
    )

    val BLUE_HOME_STRETCH: List<BoardCell> = listOf(
        BoardCell(13, 7), BoardCell(12, 7), BoardCell(11, 7), BoardCell(10, 7), BoardCell(9, 7)
    )

    // Exact fractional center coordinates (row+0.5, col+0.5) inside the 15x15 board
    private val RED_YARD_POSITIONS = listOf(
        1.85f to 1.85f,
        1.85f to 4.15f,
        4.15f to 1.85f,
        4.15f to 4.15f
    )

    private val YELLOW_YARD_POSITIONS = listOf(
        10.85f to 10.85f,
        10.85f to 13.15f,
        13.15f to 10.85f,
        13.15f to 13.15f
    )

    fun startIndexForSlot(slot: Int): Int =
        if (slot == 0) HOST_START_INDEX else GUEST_START_INDEX

    fun toTrackIndex(slot: Int, progress: Int): Int? {
        if (progress !in 1..51) return null
        return (startIndexForSlot(slot) + (progress - 1)) % 52
    }

    fun tokenBoardCenter(slot: Int, tokenIndex: Int, progress: Int): Pair<Float, Float> {
        if (progress <= 0) {
            val idx = tokenIndex.coerceIn(0, 3)
            return if (slot == 0) RED_YARD_POSITIONS[idx] else YELLOW_YARD_POSITIONS[idx]
        }
        if (progress == HOME_PROGRESS) {
            // Nest cleanly inside own goal triangle
            return if (slot == 0) {
                when (tokenIndex % 4) {
                    0 -> 7.1f to 6.35f
                    1 -> 7.9f to 6.35f
                    2 -> 7.5f to 6.65f
                    else -> 7.5f to 6.2f
                }
            } else {
                when (tokenIndex % 4) {
                    0 -> 7.1f to 8.65f
                    1 -> 7.9f to 8.65f
                    2 -> 7.5f to 8.35f
                    else -> 7.5f to 8.8f
                }
            }
        }
        if (progress in 52..56) {
            val stretch = if (slot == 0) RED_HOME_STRETCH else YELLOW_HOME_STRETCH
            val cell = stretch[progress - 52]
            return (cell.row + 0.5f) to (cell.col + 0.5f)
        }
        val trackIdx = toTrackIndex(slot, progress) ?: 0
        val cell = TRACK_COORDS[trackIdx]
        return (cell.row + 0.5f) to (cell.col + 0.5f)
    }

    fun validTokenIndices(tokens: List<Int>, dice: Int): List<Int> {
        if (dice !in 1..6) return emptyList()
        return tokens.indices.filter { idx ->
            val p = tokens[idx]
            when {
                p == 0 -> dice == 6
                p in 1 until HOME_PROGRESS -> p + dice <= HOME_PROGRESS
                else -> false
            }
        }
    }

    fun applyMove(
        activeTokens: List<Int>,
        opponentTokens: List<Int>,
        tokenIndex: Int,
        dice: Int,
        activeSlot: Int,
        activeName: String
    ): LudoMoveOutcome {
        val mutableActive = activeTokens.toMutableList()
        val mutableOpponent = opponentTokens.toMutableList()
        val currentProgress = mutableActive[tokenIndex]
        val nextProgress = if (currentProgress == 0) 1 else currentProgress + dice
        mutableActive[tokenIndex] = nextProgress

        var captured = false
        val landedTrackIdx = toTrackIndex(activeSlot, nextProgress)
        if (landedTrackIdx != null && landedTrackIdx !in SAFE_TRACK_INDICES) {
            val oppSlot = 1 - activeSlot
            for (oppIdx in mutableOpponent.indices) {
                val oppTrackIdx = toTrackIndex(oppSlot, mutableOpponent[oppIdx])
                if (oppTrackIdx == landedTrackIdx) {
                    mutableOpponent[oppIdx] = 0
                    captured = true
                }
            }
        }

        val reachedHome = nextProgress == HOME_PROGRESS
        val didWin = mutableActive.all { it == HOME_PROGRESS }
        val extraTurn = !didWin && (dice == 6 || captured || reachedHome)
        val nextTurnSlot = if (extraTurn) activeSlot else 1 - activeSlot
        val nextConsecutiveSixes = if (dice == 6 && extraTurn) 1 else 0

        val summary = when {
            didWin -> "$activeName got all 4 tokens Home & WON!"
            captured -> "$activeName captured an opponent token! Extra roll!"
            reachedHome -> "$activeName reached Home! Extra roll!"
            currentProgress == 0 -> "$activeName deployed a token on 6!"
            dice == 6 -> "$activeName moved $dice! Bonus roll!"
            else -> "$activeName rolled $dice."
        }

        return LudoMoveOutcome(
            nextActiveTokens = mutableActive,
            nextOpponentTokens = mutableOpponent,
            capturedOpponent = captured,
            reachedHome = reachedHome,
            extraTurn = extraTurn,
            didWin = didWin,
            nextTurnSlot = nextTurnSlot,
            nextConsecutiveSixes = nextConsecutiveSixes,
            summary = summary
        )
    }

    fun chooseBotToken(
        botTokens: List<Int>,
        playerTokens: List<Int>,
        dice: Int
    ): Int? {
        val valid = validTokenIndices(botTokens, dice)
        if (valid.isEmpty()) return null
        if (valid.size == 1) return valid.first()

        var bestIdx = valid.first()
        var bestScore = Int.MIN_VALUE

        for (idx in valid) {
            val cur = botTokens[idx]
            val next = if (cur == 0) 1 else cur + dice
            var score = 0

            if (next == HOME_PROGRESS) score += 1000
            if (cur == 0) score += 260
            if (cur < 52 && next >= 52) score += 320

            val nextTrack = toTrackIndex(1, next)
            if (nextTrack != null) {
                if (nextTrack in SAFE_TRACK_INDICES) score += 180
                val captures = playerTokens.count { p ->
                    val oppTrack = toTrackIndex(0, p)
                    oppTrack == nextTrack && nextTrack !in SAFE_TRACK_INDICES
                }
                if (captures > 0) score += 750 * captures

                // Penalize landing within 1..6 steps ahead of an opponent token on unsafe square
                if (nextTrack !in SAFE_TRACK_INDICES) {
                    val danger = playerTokens.any { p ->
                        val oppTrack = toTrackIndex(0, p) ?: return@any false
                        val dist = (nextTrack - oppTrack + 52) % 52
                        dist in 1..6
                    }
                    if (danger) score -= 120
                }
            }
            score += next
            // Slight bonus for escaping danger
            val curTrack = toTrackIndex(1, cur)
            if (curTrack != null && curTrack !in SAFE_TRACK_INDICES) {
                val currentlyThreatened = playerTokens.any { p ->
                    val oppTrack = toTrackIndex(0, p) ?: return@any false
                    val dist = (curTrack - oppTrack + 52) % 52
                    dist in 1..6
                }
                if (currentlyThreatened) score += 140
            }

            if (score > bestScore) {
                bestScore = score
                bestIdx = idx
            }
        }
        return bestIdx
    }

    fun normalizedTokens(raw: List<Long>): List<Int> =
        List(4) { idx -> (raw.getOrNull(idx)?.toInt() ?: 0).coerceIn(0, HOME_PROGRESS) }

    fun sameCellOffset(indexInStack: Int, totalInStack: Int): Pair<Float, Float> {
        if (totalInStack <= 1) return 0f to 0f
        return when (indexInStack % 4) {
            0 -> -0.14f to -0.14f
            1 -> 0.14f to 0.14f
            2 -> 0.14f to -0.14f
            else -> -0.14f to 0.14f
        }
    }

    fun cellDistance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
        abs(a.first - b.first) + abs(a.second - b.second)
}
