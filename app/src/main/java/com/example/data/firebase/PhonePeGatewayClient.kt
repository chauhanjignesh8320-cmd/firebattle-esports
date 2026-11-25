package com.example.data.firebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class PhonePeCreateOrderResult {
    data class Success(val merchantOrderId: String, val redirectUrl: String) : PhonePeCreateOrderResult()
    data object NotConfigured : PhonePeCreateOrderResult()
    data class Error(val message: String) : PhonePeCreateOrderResult()
}

sealed class PhonePeVerifyResult {
    data class Completed(val amount: Long, val bonusAmount: Long, val utr: String) : PhonePeVerifyResult()
    data class Pending(val amount: Long) : PhonePeVerifyResult()
    data class Failed(val message: String) : PhonePeVerifyResult()
    data class Error(val message: String) : PhonePeVerifyResult()
}

object PhonePeGatewayClient {
    private const val API_BASE_URL = "https://firebetal.com"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun createOrder(idToken: String, amountInr: Long): PhonePeCreateOrderResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("amount", amountInr)
                }.toString()
                val (code, bodyText) = postJson(
                    endpoint = "$API_BASE_URL/api/phonepe/create-order",
                    idToken = idToken,
                    jsonBody = payload
                )
                val json = JSONObject(bodyText.ifBlank { "{}" })
                if (code == 503 && json.optBoolean("notConfigured", false)) {
                    return@withContext PhonePeCreateOrderResult.NotConfigured
                }
                if (code in 200..299 && json.optBoolean("ok", false)) {
                    val orderId = json.optString("merchantOrderId")
                    val redirectUrl = json.optString("redirectUrl")
                    if (orderId.isNotBlank() && redirectUrl.isNotBlank()) {
                        return@withContext PhonePeCreateOrderResult.Success(orderId, redirectUrl)
                    }
                }
                val err = json.optString("message").ifBlank { "Unable to start PhonePe Checkout." }
                PhonePeCreateOrderResult.Error(err)
            } catch (e: Exception) {
                PhonePeCreateOrderResult.Error(e.localizedMessage ?: "Network error starting payment.")
            }
        }

    suspend fun verifyOrder(idToken: String, merchantOrderId: String): PhonePeVerifyResult =
        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("merchantOrderId", merchantOrderId)
                }.toString()
                val (code, bodyText) = postJson(
                    endpoint = "$API_BASE_URL/api/phonepe/verify-order",
                    idToken = idToken,
                    jsonBody = payload
                )
                val json = JSONObject(bodyText.ifBlank { "{}" })
                if (code in 200..299 && json.optBoolean("ok", false)) {
                    val state = json.optString("state").uppercase()
                    val amount = json.optLong("amount", 0L)
                    val bonusAmount = json.optLong("bonusAmount", 0L)
                    val utr = json.optString("utr", merchantOrderId)
                    return@withContext when (state) {
                        "COMPLETED" -> PhonePeVerifyResult.Completed(amount, bonusAmount, utr)
                        "FAILED" -> PhonePeVerifyResult.Failed(
                            json.optString("message").ifBlank { "PhonePe payment failed or cancelled." }
                        )
                        else -> PhonePeVerifyResult.Pending(amount)
                    }
                }
                val err = json.optString("message").ifBlank { "Could not verify payment status." }
                PhonePeVerifyResult.Error(err)
            } catch (e: Exception) {
                PhonePeVerifyResult.Error(e.localizedMessage ?: "Network error verifying payment.")
            }
        }

    private fun postJson(endpoint: String, idToken: String, jsonBody: String): Pair<Int, String> {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $idToken")
        }
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            conn.disconnect()
        }
    }
}
