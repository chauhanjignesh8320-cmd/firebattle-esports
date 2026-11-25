package com.example.ludo

import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.ArenaWhiteCard
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.CoinGold
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val LudoRed = Color(0xFFEF4444)
private val LudoRedDark = Color(0xFF991B1B)
private val LudoGreen = Color(0xFF10B981)
private val LudoYellow = Color(0xFFFACC15)
private val LudoYellowDark = Color(0xFFCA8A04)
private val LudoBlue = Color(0xFF3B82F6)
private val LudoBoardPaper = Color(0xFFFDFDFD)
private val LudoGridLine = Color(0xFFCBD5E1)
private val LudoSafeBg = Color(0xFFE2E8F0)

private data class RenderedToken(
    val slot: Int,
    val index: Int,
    val progress: Int,
    val targetRow: Float,
    val targetCol: Float,
    val isSelectable: Boolean
)

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

        // Apply subtle stack offsets for tokens sharing the same track/stretch cell
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

    val infiniteTransition = rememberInfiniteTransition(label = "ludo_pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val animatedCoords = renderedTokens.map { token ->
        val animRow by animateFloatAsState(
            targetValue = token.targetRow,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
            label = "row_${token.slot}_${token.index}"
        )
        val animCol by animateFloatAsState(
            targetValue = token.targetCol,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
            label = "col_${token.slot}_${token.index}"
        )
        animRow to animCol
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.5.dp, ArenaSlateBorder),
        color = LudoBoardPaper,
        shadowElevation = 6.dp,
        modifier = modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .testTag("ludo_board_canvas")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val boardPx = with(density) { maxWidth.toPx() }
            val cellPx = boardPx / 15f
            val hitSizeDp = with(density) { (cellPx * 1.45f).toDp() }.coerceAtLeast(48.dp)
            val hitSizePx = with(density) { hitSizeDp.toPx() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 15f
                drawLudoBoardGrid(cell)

                // Draw non-selectable tokens first, selectable tokens on top
                val drawOrder = renderedTokens.indices.sortedBy { if (renderedTokens[it].isSelectable) 1 else 0 }
                drawOrder.forEach { idx ->
                    val token = renderedTokens[idx]
                    val (rowF, colF) = animatedCoords[idx]
                    val center = Offset(colF * cell, rowF * cell)
                    val isStackShared = token.progress in 1 until LudoEngine.HOME_PROGRESS &&
                        renderedTokens.count {
                            it.progress in 1 until LudoEngine.HOME_PROGRESS &&
                                LudoEngine.cellDistance(it.targetRow to it.targetCol, token.targetRow to token.targetCol) < 0.35f
                        } > 1
                    val radius = if (isStackShared) cell * 0.29f else cell * 0.36f

                    if (token.isSelectable) {
                        drawCircle(
                            color = CoinGold.copy(alpha = 0.42f),
                            radius = radius * 1.45f * pulseFraction,
                            center = center
                        )
                        drawCircle(
                            color = Color.White,
                            radius = radius * 1.24f,
                            center = center,
                            style = Stroke(width = cell * 0.08f)
                        )
                    }

                    val fillColor = if (token.slot == 0) LudoRed else LudoYellow
                    val rimDark = if (token.slot == 0) LudoRedDark else LudoYellowDark

                    // Drop shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = radius,
                        center = center + Offset(0f, cell * 0.05f)
                    )
                    // Outer dark rim
                    drawCircle(
                        color = rimDark,
                        radius = radius,
                        center = center
                    )
                    // White collar
                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.84f,
                        center = center
                    )
                    // Token body
                    drawCircle(
                        color = fillColor,
                        radius = radius * 0.68f,
                        center = center
                    )
                    // Specular highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = radius * 0.24f,
                        center = center - Offset(radius * 0.22f, radius * 0.22f)
                    )
                }
            }

            // Accessible interactive hitboxes for selectable tokens
            renderedTokens.forEachIndexed { idx, token ->
                if (token.isSelectable) {
                    val (rowF, colF) = animatedCoords[idx]
                    val cx = colF * cellPx
                    val cy = rowF * cellPx
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
                                onClick = { onTokenSelected(token.index) }
                            )
                            .testTag("ludo_token_${token.slot}_${token.index}")
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawLudoBoardGrid(cell: Float) {
    // Base white paper
    drawRect(color = LudoBoardPaper, size = size)

    // 4 Home stretch lanes
    LudoEngine.RED_HOME_STRETCH.forEach { c ->
        drawRect(LudoRed.copy(alpha = 0.85f), Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.GREEN_HOME_STRETCH.forEach { c ->
        drawRect(LudoGreen.copy(alpha = 0.85f), Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.YELLOW_HOME_STRETCH.forEach { c ->
        drawRect(LudoYellow.copy(alpha = 0.88f), Offset(c.col * cell, c.row * cell), Size(cell, cell))
    }
    LudoEngine.BLUE_HOME_STRETCH.forEach { c ->
        drawRect(LudoBlue.copy(alpha = 0.85f), Offset(c.col * cell, c.row * cell), Size(cell, cell))
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
            drawRect(LudoSafeBg, Offset(c.col * cell, c.row * cell), Size(cell, cell))
        }
        drawStar(
            center = Offset((c.col + 0.5f) * cell, (c.row + 0.5f) * cell),
            outerRadius = cell * 0.30f,
            innerRadius = cell * 0.13f,
            color = if (isStart) Color.White.copy(alpha = 0.9f) else Color(0xFF475569)
        )
    }

    // Grid lines across the arms
    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val inYard = (r < 6 && c < 6) || (r < 6 && c >= 9) || (r >= 9 && c < 6) || (r >= 9 && c >= 9)
            val inCenter = r in 6..8 && c in 6..8
            if (!inYard && !inCenter) {
                drawRect(
                    color = LudoGridLine,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell),
                    style = Stroke(width = 1.2f)
                )
            }
        }
    }

    // 4 Corner Yards
    drawCornerYard(0, 0, LudoRed, cell, activeCorner = true)
    drawCornerYard(0, 9, LudoGreen, cell, activeCorner = false)
    drawCornerYard(9, 9, LudoYellow, cell, activeCorner = true)
    drawCornerYard(9, 0, LudoBlue, cell, activeCorner = false)

    // Center Goal 3x3 triangles
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
        color = ArenaInkDark.copy(alpha = 0.45f),
        topLeft = Offset(left, top),
        size = Size(3f * cell, 3f * cell),
        style = Stroke(width = 2f)
    )
    drawCircle(color = Color.White, radius = cell * 0.24f, center = Offset(cx, cy))
    drawCircle(color = CoinGold, radius = cell * 0.16f, center = Offset(cx, cy))
}

private fun DrawScope.drawCornerYard(
    startRow: Int,
    startCol: Int,
    color: Color,
    cell: Float,
    activeCorner: Boolean
) {
    val topLeft = Offset(startCol * cell, startRow * cell)
    val yardSize = Size(6f * cell, 6f * cell)
    drawRect(color = color.copy(alpha = if (activeCorner) 1f else 0.72f), topLeft = topLeft, size = yardSize)
    drawRect(color = ArenaInkDark.copy(alpha = 0.25f), topLeft = topLeft, size = yardSize, style = Stroke(width = 2f))

    val innerPadding = cell * 0.9f
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(topLeft.x + innerPadding, topLeft.y + innerPadding),
        size = Size(6f * cell - innerPadding * 2, 6f * cell - innerPadding * 2),
        cornerRadius = CornerRadius(cell * 0.45f, cell * 0.45f)
    )

    val spots = listOf(1.85f to 1.85f, 1.85f to 4.15f, 4.15f to 1.85f, 4.15f to 4.15f)
    spots.forEach { (dr, dc) ->
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = cell * 0.56f,
            center = Offset((startCol + dc) * cell, (startRow + dr) * cell)
        )
        drawCircle(
            color = color,
            radius = cell * 0.44f,
            center = Offset((startCol + dc) * cell, (startRow + dr) * cell),
            style = Stroke(width = cell * 0.08f)
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
    drawPath(path, Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.5f))
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    for (i in 0 until 10) {
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
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
    val accent = if (activeSlot == 0) LudoRed else LudoYellowDark
    val scaleAnim by animateFloatAsState(
        targetValue = if (canRoll) 1.04f else 0.96f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "dice_scale"
    )
    val spinAnim by animateFloatAsState(
        targetValue = (diceValue * 90f),
        animationSpec = tween(durationMillis = 240),
        label = "dice_spin"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Surface(
            color = if (canRoll) ArenaWhiteCard else ArenaWhiteCard.copy(alpha = 0.72f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.5.dp, if (canRoll) CoinGold else accent.copy(alpha = 0.6f)),
            shadowElevation = if (canRoll) 8.dp else 2.dp,
            modifier = Modifier
                .size(62.dp)
                .scale(scaleAnim)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = canRoll && !isBusy, onClick = onRollClick)
                .testTag("ludo_roll_dice_button")
                .semantics {
                    contentDescription = if (canRoll) "Roll Ludo dice" else "Ludo dice value $diceValue"
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (diceValue in 1..6) {
                    Canvas(
                        modifier = Modifier
                            .size(44.dp)
                            .rotate(spinAnim % 360f)
                    ) {
                        drawDicePips(diceValue, accent)
                    }
                } else {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = BlazeOrange,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when {
                    awaitingMove -> "TAP TOKEN"
                    canRoll -> "TAP TO ROLL"
                    else -> "WAIT..."
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (canRoll || awaitingMove) CoinGold else TextSecondary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (diceValue in 1..6) "Rolled: $diceValue" else "Ready",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private fun DrawScope.drawDicePips(value: Int, pipColor: Color) {
    val w = size.width
    val h = size.height
    val r = w * 0.105f
    val c1 = 0.22f
    val c2 = 0.50f
    val c3 = 0.78f

    val pips = when (value) {
        1 -> listOf(c2 to c2)
        2 -> listOf(c1 to c1, c3 to c3)
        3 -> listOf(c1 to c1, c2 to c2, c3 to c3)
        4 -> listOf(c1 to c1, c3 to c1, c1 to c3, c3 to c3)
        5 -> listOf(c1 to c1, c3 to c1, c2 to c2, c1 to c3, c3 to c3)
        else -> listOf(c1 to c1, c3 to c1, c1 to c2, c3 to c2, c1 to c3, c3 to c3)
    }
    pips.forEach { (fx, fy) ->
        drawCircle(color = pipColor, radius = r, center = Offset(w * fx, h * fy))
    }
}
