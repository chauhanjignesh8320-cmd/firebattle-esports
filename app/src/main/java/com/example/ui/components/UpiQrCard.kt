package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ArenaInkDark
import com.example.ui.theme.ArenaInkMuted
import com.example.ui.theme.ArenaLightBorder
import com.example.ui.theme.ArenaLightSurface
import com.example.ui.theme.ArenaObsidian
import com.example.ui.theme.ArenaSlateBorder
import com.example.ui.theme.BlazeOrange
import com.example.ui.theme.BlazeOrangeContainer
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.EmeraldWinContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

data class UpiIntentOutcome(
    val isSuccess: Boolean,
    val isExplicitFailure: Boolean,
    val referenceId: String,
    val statusLabel: String
)

fun generateUpiTxnRef(): String {
    val timePart = System.currentTimeMillis().toString(36).uppercase().takeLast(7)
    val randPart = (100..999).random().toString()
    return "FB$timePart$randPart"
}

fun parseUpiIntentResponse(rawResponse: String?): UpiIntentOutcome {
    if (rawResponse.isNullOrBlank()) {
        return UpiIntentOutcome(
            isSuccess = false,
            isExplicitFailure = false,
            referenceId = "",
            statusLabel = "UNKNOWN"
        )
    }
    val params = rawResponse.split("&")
        .mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx > 0) {
                part.substring(0, idx).trim().lowercase() to Uri.decode(part.substring(idx + 1).trim())
            } else {
                null
            }
        }
        .toMap()
    val status = (params["status"] ?: "").uppercase()
    val responseCode = (params["responsecode"] ?: "").trim()
    val refId = (params["approvalrefno"] ?: params["txnref"] ?: params["txnid"] ?: "")
        .filter { it.isLetterOrDigit() }
        .take(32)
    val success = status == "SUCCESS" || status == "SUBMITTED" || responseCode == "00"
    val explicitFailure = status == "FAILURE" || status == "FAILED" || status == "CANCELLED" || status == "DECLINED"
    return UpiIntentOutcome(
        isSuccess = success,
        isExplicitFailure = explicitFailure,
        referenceId = refId,
        statusLabel = status.ifBlank { if (responseCode == "00") "SUCCESS" else "UNKNOWN" }
    )
}

fun buildUpiPaymentUri(
    upiId: String,
    upiName: String,
    amount: Long? = null,
    transactionNote: String = "Firebattle Wallet Deposit",
    transactionRef: String = ""
): String {
    val builder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId.trim())
        .appendQueryParameter("pn", upiName.trim().ifBlank { "Firebattle Esports" })
        .appendQueryParameter("cu", "INR")
    if (amount != null && amount > 0L) {
        builder.appendQueryParameter("am", "$amount.00")
    }
    if (transactionRef.isNotBlank()) {
        builder.appendQueryParameter("tr", transactionRef.trim())
    }
    builder.appendQueryParameter("tn", transactionNote)
    return builder.build().toString()
}

private fun generateQrImageBitmap(content: String, sizePx: Int = 384): ImageBitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun UpiQrPaymentCard(
    upiId: String,
    upiName: String,
    amount: Long?,
    onFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLightSurface: Boolean = false,
    onUpiRefCaptured: (String) -> Unit = {},
    onAutoSubmitUpiDeposit: ((Long, String) -> Unit)? = null,
    onReturnedFromUpiApp: ((Long, String, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val cleanUpi = upiId.trim()
    val hasValidUpi = cleanUpi.length >= 3 && cleanUpi.contains("@")
    val pendingRefState = rememberSaveable { mutableStateOf("") }
    val pendingAmountState = rememberSaveable { mutableStateOf(0L) }
    val upiUri = remember(cleanUpi, upiName, amount) {
        if (hasValidUpi) buildUpiPaymentUri(cleanUpi, upiName, amount) else ""
    }
    val qrBitmap = remember(upiUri) {
        if (upiUri.isNotBlank()) generateQrImageBitmap(upiUri) else null
    }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val raw = result.data?.getStringExtra("response")
        val outcome = parseUpiIntentResponse(raw)
        val launchedAmount = pendingAmountState.value.takeIf { it > 0L } ?: (amount ?: 0L)
        val resolvedRef = outcome.referenceId.takeIf { it.length >= 6 }
            ?: pendingRefState.value.takeIf { it.length >= 6 }
            ?: generateUpiTxnRef()

        when {
            (outcome.isSuccess || (result.resultCode == Activity.RESULT_OK && !outcome.isExplicitFailure)) &&
                launchedAmount > 0L &&
                onAutoSubmitUpiDeposit != null -> {
                onUpiRefCaptured(resolvedRef)
                onAutoSubmitUpiDeposit(launchedAmount, resolvedRef)
            }
            outcome.isExplicitFailure -> {
                onReturnedFromUpiApp?.invoke(launchedAmount, "", true)
                onFeedback("UPI payment ${outcome.statusLabel.lowercase()}. Request was not sent.")
            }
            else -> {
                onUpiRefCaptured(resolvedRef)
                onReturnedFromUpiApp?.invoke(launchedAmount, resolvedRef, false)
                onFeedback("Returned from UPI app — tap 'Yes, Paid' below if ₹$launchedAmount was deducted!")
            }
        }
    }

    val cardBg = if (isLightSurface) ArenaLightSurface else ArenaObsidian
    val cardBorder = if (isLightSurface) ArenaLightBorder else EmeraldWin.copy(alpha = 0.35f)
    val hintTextColor = if (isLightSurface) ArenaInkMuted else TextSecondary
    val titleColor = if (isLightSurface) ArenaInkDark else EmeraldWin

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = EmeraldWin,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.upi_qr_scan_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor,
                    fontWeight = FontWeight.Bold
                )
            }

            if (amount != null && amount > 0L) {
                Surface(
                    color = if (isLightSurface) BlazeOrange.copy(alpha = 0.12f) else BlazeOrangeContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Amount Pre-filled: ₹$amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = BlazeOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (qrBitmap != null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = if (isLightSurface) BorderStroke(1.dp, ArenaLightBorder) else null,
                    shadowElevation = 2.dp
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "UPI Payment QR Code for $cleanUpi",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(176.dp)
                            .testTag("upi_qr_code_image")
                    )
                }

                Text(
                    text = stringResource(R.string.upi_qr_scan_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = hintTextColor,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        if (amount == null || amount <= 0L) {
                            onFeedback("Select a quick pack or enter Amount (₹) first before opening UPI app.")
                            return@Button
                        }
                        val txnRef = generateUpiTxnRef()
                        pendingRefState.value = txnRef
                        pendingAmountState.value = amount
                        val payUri = buildUpiPaymentUri(
                            upiId = cleanUpi,
                            upiName = upiName,
                            amount = amount,
                            transactionRef = txnRef
                        )
                        val payIntent = Intent(Intent.ACTION_VIEW, Uri.parse(payUri))
                        try {
                            upiLauncher.launch(Intent.createChooser(payIntent, "Pay ₹$amount via UPI App"))
                        } catch (_: ActivityNotFoundException) {
                            onFeedback("No UPI app found on this phone. Scan QR from another phone or copy UPI ID.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlazeOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_upi_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (amount != null && amount > 0L) {
                            "Pay ₹$amount via UPI App"
                        } else {
                            stringResource(R.string.open_upi_app)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Organizer UPI ID is not configured yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun AdminUpiQrPreviewCard(
    upiId: String,
    upiName: String,
    modifier: Modifier = Modifier
) {
    val cleanUpi = upiId.trim()
    val hasValidUpi = cleanUpi.length >= 3 && cleanUpi.contains("@")
    val upiUri = remember(cleanUpi, upiName) {
        if (hasValidUpi) buildUpiPaymentUri(cleanUpi, upiName, null) else ""
    }
    val qrBitmap = remember(upiUri) {
        if (upiUri.isNotBlank()) generateQrImageBitmap(upiUri, 320) else null
    }

    Surface(
        color = ArenaObsidian,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArenaSlateBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.admin_qr_preview_title),
                style = MaterialTheme.typography.titleSmall,
                color = EmeraldWin,
                fontWeight = FontWeight.Bold
            )

            if (qrBitmap != null) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Live Admin UPI QR Preview for $cleanUpi",
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(152.dp)
                            .testTag("admin_upi_qr_preview")
                    )
                }
                Text(
                    text = "$upiName • $cleanUpi",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Surface(
                    color = EmeraldWinContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Shown automatically on Player Wallet Deposit screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldWin,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    text = "Enter a valid UPI ID above (e.g. name@ybl) to generate your live payment QR.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
