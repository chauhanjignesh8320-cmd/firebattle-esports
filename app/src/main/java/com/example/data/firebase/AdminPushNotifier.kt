package com.example.data.firebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AdminPushNotifier {
    private const val ENDPOINT = "https://firebetal.com/api/notify-admin"
    private const val TIMEOUT_MS = 10_000

    suspend fun notifyNewDeposit(idToken: String, depositId: String) {
        if (idToken.isBlank() || depositId.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("depositId", depositId)
                }.toString()
                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                try {
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(payload)
                        writer.flush()
                    }
                    conn.responseCode
                } finally {
                    conn.disconnect()
                }
            }
        }
    }
}
