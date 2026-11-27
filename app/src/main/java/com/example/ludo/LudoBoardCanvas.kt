package com.example.ludo

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaSlateDark
import com.example.ui.theme.CoinGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Classic Original Ludo Palette
private val LudoWoodOuter = Color(0xFF5A2B0C)
private val LudoWoodInner = Color(0xFF8C4A1C)
private val LudoGoldTrim = Color(0xFFF6C744)
private val LudoBoardPaper = Color(0xFFFFFDF7)
private val LudoGridInk = Color(0xFF1E293B)
private val LudoSafeTileBg = Color(0xFFECEFF4)

private val LudoRed = Color(0xFFE52521)
private val LudoRedDark = Color(0xFF8F110E)
private val LudoRedLight = Color(0xFFFF7A76)

private val LudoGreen = Color(0xFF16A34A)
private val LudoGreenDark = Color(0xFF0E682F)

private val LudoYellow = Color(0xFFFACC15)
private val LudoYellowDark = Color(0xFFB45309)
private val LudoYellowLight = Color(0xFFFEF08A)

private val LudoBlue = Color(0xFF2563EB)
private val LudoBlueDark = Color(0xFF1D4ED8)

private val DiceIvoryTop = Color(0xFFFFFFFF)
private val DiceIvoryFace = Color(0xFFF8F6EE)
private val DiceIvoryBevel = Color(0xFFD6D3C7)
private val DicePipDark = Color(0xFF1E293B)

private data class RenderedToken(
    val slot: Int,
    val index: Int,
    val progress: Int,
    val targetRow: Float,
    val targetCol: Float,
    val isSelectable: Boolean
)

private class TokenVisualState(
    initialProgress: Int,
    initialRow: Float,
    initialCol: Float
) {
    var lastProgress by mutableIntStateOf(initialProgress)
    var currentRow by mutableFloatStateOf(initialRow)
    var currentCol by mutableFloatStateOf(initialCol)
    var hopLift by mutableFloatStateOf(0f)
    var isHopping by mutableStateOf(false)
}

@Composable
fun LudoBoardCanvas(
    hostTokens: List<Int>,
    guestTokens: List<Int>,
    activeTurnSlot: Int,
    diceValue: Int,
    awaitingMove: Boolean,
    canInteract: Boolean,
    onTokenSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val validIndices = remember(hostTokens, guestTokens, activeTurnSlot, diceValue, awaitingMove, canInteract) {
        if (!awaitingMove || !canInteract) emptySet()
        else {
            val active = if (activeTurnSlot == 0) hostTokens else guestTokens
            LudoEngine.validTokenIndices(active, diceValue).toSet()
        }
    }

    val renderedTokens = remember(hostTokens, guestTokens, activeTurnSlot, validIndices) {
        val raw = buildList {
            hostTokens.forEachIndexed { idx, prog ->
                val (r, c) = LudoEngine.tokenBoardCenter(0, idx, prog)
                add(RenderedToken(0, idx, prog, r, c, activeTurnSlot == 0 && idx in validIndices))
            }
            guestTokens.forEachIndexed { idx, prog ->
                val (r, c) = LudoEngine.tokenBoardCenter(1, idx, prog)
                add(RenderedToken(1, idx, prog, r, c, activeTurnSlot == 1 && idx in validIndices))
            }
        }

        val groupedByCell = raw
            .filter { it.progress in 1 until LudoEngine.HOME_PROGRESS }
            .groupBy { (it.targetRow * 10).roundToInt() to (it.targetCol * 10).roundToInt() }

        raw.map { token ->
            if (token.progress in 1 until LudoEngine.HOME_PROGRESS) {
                val key = (token.targetRow * 10).roundToInt() to (token.targetCol * 10).roundToInt()
                val stack = groupedByCell[key].orEmpty()
                val posInStack = stack.indexOfFirst { it.slot == token.slot && it.index == token.index }.coerceAtLeast(0)
                val (offR, offC) = LudoEngine.sameCellOffset(posInStack, stack.size)
                token.copy(targetRow = token.targetRow + offR, targetCol = token.targetCol + offC)
            } else {
                token
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ludo_classic_pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    val allTokensZero = remember(hostTokens, guestTokens) {
        hostTokens.all { it == 0 } && guestTokens.all { it == 0 }
    }

    val visualStates = renderedTokens.map { token ->
        val state = remember(token.slot, token.index) {
            TokenVisualState(
                initialProgress = token.progress,
                initialRow = token.targetRow,
                initialCol = token.targetCol
            )
        }
        val latestTargetRow by rememberUpdatedState(token.targetRow)
        val latestTargetCol by rememberUpdatedState(token.targetCol)
        val latestAllZero by rememberUpdatedState(allTokensZero)

        LaunchedEffect(token.slot, token.index, token.progress) {
            val prevProg = state.lastProgress
            val newProg = token.progress
            if (prevProg == newProg) {
                state.currentRow = latestTargetRow
                state.currentCol = latestTargetCol
                state.hopLift = 0f
                state.isHopping = false
                return@LaunchedEffect
            }

            state.lastProgress = newProg
            state.isHopping = true
            try {
                when {
                    newProg > prevProg -> {
                        val startStep = if (prevProg <= 0) 1 else prevProg + 1
                        for (step in startStep..newProg) {
                            val isFinalStep = (step == newProg)
                            val (endR, endC) = if (isFinalStep) {
                                latestTargetRow to latestTargetCol
                            } else {
                                LudoEngine.tokenBoardCenter(token.slot, token.index, step)
                            }
                            val startR = state.currentRow
                            val startC = state.currentCol
                            val stepDuration = if (prevProg <= 0 && step == 1 && isFinalStep) 200 else 108
                            animate(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = stepDuration,
                                    easing = FastOutSlowInEasing
                                )
                            ) { t, _ ->
                                state.currentRow = startR + (endR - startR) * t
                                state.currentCol = startC + (endC - startC) * t
                                state.hopLift = sin(t * PI).toFloat()
                            }
                            state.hopLift = 0f
                        }
                    }

                    newProg == 0 && !latestAllZero -> {
                        val startR = state.currentRow
                        val startC = state.currentCol
                        val endR = latestTargetRow
                        val endC = latestTargetCol
                        animate(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        ) { t, _ ->
                            state.currentRow = startR + (endR - startR) * t
                            state.currentCol = startC + (endC - startC) * t
                            state.hopLift = (sin(t * PI) * 0.85f).toFloat()
                        }
                    }

                    else -> {
                        state.currentRow = latestTargetRow
                        state.currentCol = latestTargetCol
                    }
                }
            } finally {
                state.hopLift = 0f
                state.isHopping = false
            }
        }

        LaunchedEffect(token.slot, token.index, token.targetRow, token.targetCol) {
            if (!state.isHopping && state.lastProgress == token.progress) {
                val startR = state.currentRow
                val startC = state.currentCol
                val endR = token.targetRow
                val endC = token.targetCol
                if (kotlin.math.abs(startR - endR) > 0.005f || kotlin.math.abs(startC - endC) > 0.005f) {
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
                    ) { t, _ ->
                        state.currentRow = startR + (endR - startR) * t
                        state.currentCol = startC + (endC - startC) * t
                    }
                }
            }
        }
        state
    }

    // Classic Wooden Bezel Container
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(3.dp, LudoWoodOuter),
        color = LudoWoodInner,
        shadowElevation = 10.dp,
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .testTag("ludo_board_canvas")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFA05A28), LudoWoodInner, LudoWoodOuter)
                    )
                )
                .padding(7.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.8.dp, LudoGoldTrim),
                color = LudoBoardPaper,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val boardPx = with(density) { maxWidth.toPx() }
                    val cellPx = boardPx / 15f
                    val hitSizeDp = with(density) { (cellPx * 1.48f).toDp() }.coerceAtLeast(48.dp)
                    val hitSizePx = with(density) { hitSizeDp.toPx() }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cell = size.width / 15f
                        drawOriginalLudoBoard(cell = cell, activeTurnSlot = activeTurnSlot)

                        // Draw yard & stationary tokens first; selectable & hopping tokens on top; vertical order so pins overlap naturally
                        val drawOrder = renderedTokens.indices.sortedWith(
                            compareBy<Int> {
                                (if (renderedTokens[it].isSelectable) 2 else 0) +
                                    (if (visualStates[it].hopLift > 0.02f) 4 else 0)
                            }.thenBy { visualStates[it].currentRow }
                        )

                        drawOrder.forEach { idx ->
                            val token = renderedTokens[idx]
                            val vs = visualStates[idx]
                            val groundCenter = Offset(vs.currentCol * cell, vs.currentRow * cell)
                            val isStackShared = token.progress in 1 until LudoEngine.HOME_PROGRESS &&
                                renderedTokens.count {
                                    it.progress in 1 until LudoEngine.HOME_PROGRESS &&
                                        LudoEngine.cellDistance(
                                            it.targetRow to it.targetCol,
                                            token.targetRow to token.targetCol
                                        ) < 0.35f
                                } > 1

                            drawClassicGotiPawn(
                                groundCenter = groundCenter,
                                cell = cell,
                                slot = token.slot,
                                lift = vs.hopLift,
                                isSelectable = token.isSelectable,
                                isStackShared = isStackShared,
                                pulseFraction = pulseFraction,
                                haloRotation = haloRotation
                            )
                        }
                    }

                    // Accessible interactive hitboxes for selectable tokens
                    renderedTokens.forEachIndexed { idx, token ->
                        if (token.isSelectable) {
                            val vs = visualStates[idx]
                            val cx = vs.currentCol * cellPx
                            val cy = vs.currentRow * cellPx
                            val leftPx = (cx - hitSizePx / 2f).roundToInt()
                            val topPx = (cy - hitSizePx / 2f).roundToInt()

                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(leftPx, topPx) }
                                    .size(hitSizeDp)
                                    .semantics {
                                        contentDescription = "Move token ${token.index + 1}"
                                    }
                                    .clickable(
                                        role = Role.Button,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onTokenSelected(token.index)
                                        }
                                    )
                                    .testTag("ludo_token_${token.slot}_${token.index}")
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawClassicGotiPawn(
    groundCenter: Offset,
    cell: Float,
    slot: Int,
    lift: Float,
    isSelectable: Boolean,
    isStackShared: Boolean,
    pulseFraction: Float,
    haloRotation: Float
) {
    val baseRadius = if (isStackShared) cell * 0.27f else cell * 0.34f
    val scale = 1f + 0.16f * lift
    val r = baseRadius * scale
    val subtleHover = if (isSelectable && lift < 0.01f) (pulseFraction - 1f) * cell * 0.16f else 0f
    val hopY = cell * 0.34f * lift + subtleHover

    val fillColor = if (slot == 0) LudoRed else LudoYellow
    val darkColor = if (slot == 0) LudoRedDark else LudoYellowDark
    val lightColor = if (slot == 0) LudoRedLight else LudoYellowLight

    // Rotating classic selection halo under the token base
    if (isSelectable) {
        drawCircle(
            color = CoinGold.copy(alpha = 0.26f),
            radius = r * 1.48f * pulseFraction,
            center = groundCenter
        )
        rotate(degrees = haloRotation, pivot = groundCenter) {
            drawCircle(
                color = LudoGoldTrim,
                radius = r * 1.32f,
                center = groundCenter,
                style = Stroke(
                    width = cell * 0.075f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(cell * 0.22f, cell * 0.14f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }
    }

    // Ground shadow stays anchored on board surface
    drawOval(
        color = Color.Black.copy(alpha = (0.30f - 0.12f * lift).coerceAtLeast(0.10f)),
        topLeft = Offset(
            groundCenter.x - baseRadius * 0.92f,
            groundCenter.y - baseRadius * 0.35f + cell * 0.12f
        ),
        size = Size(baseRadius * 1.84f, baseRadius * 0.92f)
    )

    // Pedestal base rim
    val baseCenter = Offset(groundCenter.x, groundCenter.y - hopY + r * 0.10f)
    drawCircle(color = darkColor, radius = r * 0.96f, center = baseCenter)
    drawCircle(color = Color.White, radius = r * 0.84f, center = baseCenter)
    drawCircle(color = fillColor, radius = r * 0.72f, center = baseCenter)

    // 3D Teardrop Pawn Body rising upward from pedestal
    val headCenter = Offset(groundCenter.x, groundCenter.y - hopY - r * 0.42f)
    val headRadius = r * 0.66f
    val pinPath = Path().apply {
        moveTo(baseCenter.x, baseCenter.y + r * 0.32f)
        cubicTo(
            baseCenter.x - headRadius * 1.15f,
            baseCenter.y - r * 0.08f,
            headCenter.x - headRadius * 1.05f,
            headCenter.y - headRadius * 0.22f,
            headCenter.x,
            headCenter.y - headRadius
        )
        cubicTo(
            headCenter.x + headRadius * 1.05f,
            headCenter.y - headRadius * 0.22f,
            baseCenter.x + headRadius * 1.15f,
            baseCenter.y - r * 0.08f,
            baseCenter.x,
            baseCenter.y + r * 0.32f
        )
        close()
    }

    drawPath(
        path = pinPath,
        brush = Brush.verticalGradient(
            colors = listOf(lightColor, fillColor, darkColor),
            startY = headCenter.y - headRadius,
            endY = baseCenter.y + r * 0.32f
        )
    )
    drawPath(
        path = pinPath,
        color = darkColor.copy(alpha = 0.85f),
        style = Stroke(width = (cell * 0.045f).coerceAtLeast(1.2f), join = StrokeJoin.Round)
    )

    // Classic white inner ring + jewel center on top of pawn head
    drawCircle(color = Color.White, radius = headRadius * 0.56f, center = headCenter)
    drawCircle(color = fillColor, radius = headRadius * 0.38f, center = headCenter)

    // Glossy specular shine
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = headRadius * 0.16f,
        center = Offset(headCenter.x - headRadius * 0.28f, headCenter.y - headRadius * 0.30f)
    )
}

private fun DrawScope.drawOriginalLudoBoard(cell: Float, activeTurnSlot: Int) {
    // Ivory Paper Background
    drawRect(color = LudoBoardPaper, size = size)

    // 4 Home stretch lanes (rich classic colors)
    LudoEngine.RED_HOME_STRETCH.forEach { c ->
        drawRect(LudoRed, Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.GREEN_HOME_STRETCH.forEach { c ->
        drawRect(LudoGreen, Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.YELLOW_HOME_STRETCH.forEach { c ->
        drawRect(LudoYellow, Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.BLUE_HOME_STRETCH.forEach { c ->
        drawRect(LudoBlue, Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }

    // Start squares
    val startColors = mapOf(
        0 to LudoRed,
        13 to LudoGreen,
        26 to LudoYellow,
        39 to LudoBlue
    )
    startColors.forEach { (idx, color) ->
        val c = LudoEngine.TRACK_COORDS[idx]
        drawRect(color, Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }

    // Safe star cells
    LudoEngine.SAFE_TRACK_INDICES.forEach { idx ->
        val c = LudoEngine.TRACK_COORDS[idx]
        val isStart = idx in startColors
        if (!isStart) {
            drawRect(LudoSafeTileBg, Offset(c.col * cell, c.row * cell), Size(cell, cell))
        }
        val center = Offset((c.col + 0.5f) * cell, (c.row + 0.5f) * cell)
        if (isStart) {
            drawStar(
                center = center,
                outerRadius = cell * 0.34f,
                innerRadius = cell * 0.15f,
                color = Color.White.copy(alpha = 0.94f)
            )
        } else {
            drawStar(
                center = center,
                outerRadius = cell * 0.31f,
                innerRadius = cell * 0.135f,
                color = Color(0xFF94A3B8).copy(alpha = 0.38f)
            )
            drawStarOutline(
                center = center,
                outerRadius = cell * 0.31f,
                innerRadius = cell * 0.135f,
                color = Color(0xFF475569),
                strokeWidth = (cell * 0.045f).coerceAtLeast(1.2f)
            )
        }
    }

    // Home-stretch directional entry arrows on outer middle track tiles
    drawDirectionArrow(center = Offset(0.5f * cell, 7.5f * cell), cell = cell, angleDeg = 0f, color = LudoRed)
    drawDirectionArrow(center = Offset(7.5f * cell, 0.5f * cell), cell = cell, angleDeg = 90f, color = LudoGreen)
    drawDirectionArrow(center = Offset(14.5f * cell, 7.5f * cell), cell = cell, angleDeg = 180f, color = LudoYellowDark)
    drawDirectionArrow(center = Offset(7.5f * cell, 14.5f * cell), cell = cell, angleDeg = 270f, color = LudoBlue)

    // Crisp classic ink grid lines across the arms
    val gridStroke = (cell * 0.036f).coerceAtLeast(1.1f)
    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val inYard = (r < 6 && c < 6) || (r < 6 && c >= 9) || (r >= 9 && c < 6) || (r >= 9 && c >= 9)
            val inCenter = r in 6..8 && c in 6..8
            if (!inYard && !inCenter) {
                drawRect(
                    color = LudoGridInk.copy(alpha = 0.72f),
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell),
                    style = Stroke(width = gridStroke)
                )
            }
        }
    }

    // 4 Corner Yards (Full classic saturation + subtle active turn border glow for active players)
    drawCornerYard(0, 0, LudoRed, LudoRedDark, cell, isActiveCorner = activeTurnSlot == 0)
    drawCornerYard(0, 9, LudoGreen, LudoGreenDark, cell, isActiveCorner = false)
    drawCornerYard(9, 9, LudoYellow, LudoYellowDark, cell, isActiveCorner = activeTurnSlot == 1)
    drawCornerYard(9, 0, LudoBlue, LudoBlueDark, cell, isActiveCorner = false)

    // Center 3x3 Home Goal Triangles
    val cx = 7.5f * cell
    val cy = 7.5f * cell
    val left = 6f * cell
    val top = 6f * cell
    val right = 9f * cell
    val bottom = 9f * cell

    drawTriangle(Offset(left, top), Offset(left, bottom), Offset(cx, cy), LudoRed)
    drawTriangle(Offset(left, top), Offset(right, top), Offset(cx, cy), LudoGreen)
    drawTriangle(Offset(right, top), Offset(right, bottom), Offset(cx, cy), LudoYellow)
    drawTriangle(Offset(left, bottom), Offset(right, bottom), Offset(cx, cy), LudoBlue)

    drawRect(
        color = LudoGridInk,
        topLeft = Offset(left, top),
        size = Size(3f * cell, 3f * cell),
        style = Stroke(width = (cell * 0.06f).coerceAtLeast(1.6f))
    )

    // Classic Golden Center Medallion + Crown
    drawCircle(color = LudoGridInk, radius = cell * 0.34f, center = Offset(cx, cy))
    drawCircle(color = Color.White, radius = cell * 0.30f, center = Offset(cx, cy))
    drawCircle(color = LudoGoldTrim, radius = cell * 0.24f, center = Offset(cx, cy))
    drawMiniCrown(center = Offset(cx, cy), radius = cell * 0.16f, color = LudoWoodOuter)
}

private fun DrawScope.drawCornerYard(
    startRow: Int,
    startCol: Int,
    color: Color,
    darkColor: Color,
    cell: Float,
    isActiveCorner: Boolean
) {
    val topLeft = Offset(startCol * cell, startRow * cell)
    val yardSize = Size(6f * cell, 6f * cell)
    drawRect(color = color, topLeft = topLeft, size = yardSize)
    drawRect(
        color = if (isActiveCorner) LudoGoldTrim else LudoGridInk.copy(alpha = 0.85f),
        topLeft = topLeft,
        size = yardSize,
        style = Stroke(width = if (isActiveCorner) cell * 0.10f else cell * 0.05f)
    )

    val innerPadding = cell * 0.85f
    val pocketTopLeft = Offset(topLeft.x + innerPadding, topLeft.y + innerPadding)
    val pocketSize = Size(6f * cell - innerPadding * 2, 6f * cell - innerPadding * 2)

    // Subtle pocket shadow
    drawRoundRect(
        color = darkColor.copy(alpha = 0.45f),
        topLeft = pocketTopLeft + Offset(0f, cell * 0.07f),
        size = pocketSize,
        cornerRadius = CornerRadius(cell * 0.50f, cell * 0.50f)
    )
    drawRoundRect(
        color = Color.White,
        topLeft = pocketTopLeft,
        size = pocketSize,
        cornerRadius = CornerRadius(cell * 0.50f, cell * 0.50f)
    )
    drawRoundRect(
        color = LudoGridInk.copy(alpha = 0.35f),
        topLeft = pocketTopLeft,
        size = pocketSize,
        cornerRadius = CornerRadius(cell * 0.50f, cell * 0.50f),
        style = Stroke(width = (cell * 0.035f).coerceAtLeast(1f))
    )

    val spots = listOf(1.85f to 1.85f, 1.85f to 4.15f, 4.15f to 1.85f, 4.15f to 4.15f)
    spots.forEach { (dr, dc) ->
        val spotCenter = Offset((startCol + dc) * cell, (startRow + dr) * cell)
        drawCircle(
            color = color.copy(alpha = 0.16f),
            radius = cell * 0.56f,
            center = spotCenter
        )
        drawCircle(
            color = color,
            radius = cell * 0.47f,
            center = spotCenter,
            style = Stroke(width = cell * 0.09f)
        )
        drawCircle(
            color = darkColor.copy(alpha = 0.25f),
            radius = cell * 0.38f,
            center = spotCenter,
            style = Stroke(width = cell * 0.03f)
        )
    }
}

private fun DrawScope.drawTriangle(p1: Offset, p2: Offset, p3: Offset, color: Color) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(path, color)
    drawPath(path, LudoGridInk.copy(alpha = 0.65f), style = Stroke(width = 1.8f))
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = buildStarPath(center, outerRadius, innerRadius)
    drawPath(path, color)
}

private fun DrawScope.drawStarOutline(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = buildStarPath(center, outerRadius, innerRadius)
    drawPath(path, color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
}

private fun buildStarPath(center: Offset, outerRadius: Float, innerRadius: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawDirectionArrow(
    center: Offset,
    cell: Float,
    angleDeg: Float,
    color: Color
) {
    val s = cell * 0.25f
    rotate(degrees = angleDeg, pivot = center) {
        val path = Path().apply {
            moveTo(center.x - s * 0.85f, center.y)
            lineTo(center.x + s * 0.75f, center.y)
            moveTo(center.x + s * 0.15f, center.y - s * 0.58f)
            lineTo(center.x + s * 0.82f, center.y)
            lineTo(center.x + s * 0.15f, center.y + s * 0.58f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = (cell * 0.08f).coerceAtLeast(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawMiniCrown(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius, center.y + radius * 0.65f)
        lineTo(center.x - radius * 1.05f, center.y - radius * 0.45f)
        lineTo(center.x - radius * 0.45f, center.y + radius * 0.08f)
        lineTo(center.x, center.y - radius * 0.82f)
        lineTo(center.x + radius * 0.45f, center.y + radius * 0.08f)
        lineTo(center.x + radius * 1.05f, center.y - radius * 0.45f)
        lineTo(center.x + radius, center.y + radius * 0.65f)
        close()
    }
    drawPath(path, color)
}

@Composable
fun LudoDiceControl(
    diceValue: Int,
    activeSlot: Int,
    canRoll: Boolean,
    awaitingMove: Boolean,
    isBusy: Boolean,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val accent = if (activeSlot == 0) LudoRed else LudoYellowDark

    // Tumble animation whenever a real face value settles or changes
    var displayedFace by remember { mutableIntStateOf(diceValue.coerceIn(0, 6)) }
    var tumbleAngle by remember { mutableFloatStateOf(0f) }
    var tumbleScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(diceValue, awaitingMove) {
        if (diceValue in 1..6) {
            val previewFaces = listOf(
                ((diceValue % 6) + 1),
                (((diceValue + 2) % 6) + 1),
                (((diceValue + 4) % 6) + 1)
            )
            tumbleScale = 1.15f
            previewFaces.forEachIndexed { stepIdx, face ->
                displayedFace = face
                tumbleAngle = (stepIdx + 1) * 90f
                delay(52L)
            }
            displayedFace = diceValue
            tumbleAngle = 360f
            tumbleScale = 1f
        } else {
            displayedFace = 0
            tumbleAngle = 0f
            tumbleScale = 1f
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "dice_ready_pulse")
    val readyPulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_pulse"
    )
    val animatedSpin by animateFloatAsState(
        targetValue = tumbleAngle,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "dice_spin"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (canRoll) readyPulse * tumbleScale else 0.96f * tumbleScale,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "dice_scale"
    )

    Surface(
        color = ArenaSlateDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (canRoll) 2.dp else 1.dp,
            color = if (canRoll) LudoGoldTrim else ArenaSlateBorder
        ),
        shadowElevation = if (canRoll) 6.dp else 1.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Classic 3D Ivory Dice Cube
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .scale(animatedScale)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DiceIvoryTop, DiceIvoryFace, DiceIvoryBevel)
                        )
                    )
                    .clickable(
                        enabled = canRoll && !isBusy,
                        role = Role.Button,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRollClick()
                        }
                    )
                    .testTag("ludo_roll_dice_button")
                    .semantics {
                        contentDescription = if (canRoll) "Roll Ludo dice" else "Ludo dice value $diceValue"
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .rotate(animatedSpin % 360f)
                ) {
                    // Inner beveled ivory cube border
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(size.width * 0.22f, size.width * 0.22f)
                    )
                    drawRoundRect(
                        color = if (canRoll) accent.copy(alpha = 0.75f) else DiceIvoryBevel,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(size.width * 0.22f, size.width * 0.22f),
                        style = Stroke(width = size.width * 0.055f)
                    )
                    if (displayedFace in 1..6) {
                        drawClassicDicePips(displayedFace)
                    }
                }
                if (displayedFace !in 1..6) {
                    Text(
                        text = "ROLL",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = when {
                        awaitingMove -> "TAP GOTI"
                        canRoll -> "ROLL DICE"
                        else -> "WAIT..."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canRoll || awaitingMove) LudoGoldTrim else TextSecondary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (diceValue in 1..6) "Rolled: $diceValue" else "Your Turn",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (canRoll || awaitingMove) TextPrimary else TextSecondary
                )
            }
        }
    }
}

private fun DrawScope.drawClassicDicePips(value: Int) {
    val w = size.width
    val h = size.height
    val r = if (value == 1) w * 0.14f else w * 0.10f
    val c1 = 0.25f
    val c2 = 0.50f
    val c3 = 0.75f

    val pips = when (value) {
        1 -> listOf(c2 to c2)
        2 -> listOf(c1 to c1, c3 to c3)
        3 -> listOf(c1 to c1, c2 to c2, c3 to c3)
        4 -> listOf(c1 to c1, c3 to c1, c1 to c3, c3 to c3)
        5 -> listOf(c1 to c1, c3 to c1, c2 to c2, c1 to c3, c3 to c3)
        else -> listOf(c1 to c1, c3 to c1, c1 to c2, c3 to c2, c1 to c3, c3 to c3)
    }
    val pipColor = if (value == 1) LudoRed else DicePipDark
    pips.forEach { (fx, fy) ->
        val center = Offset(w * fx, h * fy)
        // Subtle recessed pip shadow + sheen
        drawCircle(color = Color.Black.copy(alpha = 0.22f), radius = r * 1.05f, center = center + Offset(0f, r * 0.14f))
        drawCircle(color = pipColor, radius = r, center = center)
        drawCircle(color = Color.White.copy(alpha = 0.25f), radius = r * 0.30f, center = center - Offset(r * 0.24f, r * 0.24f))
    }
}
