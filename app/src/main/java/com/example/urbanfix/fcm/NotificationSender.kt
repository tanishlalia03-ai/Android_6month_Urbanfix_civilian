package com.example.urbanfix.fcm

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object NotificationSender {
    private const val PROJECT_ID = "practise-4169a"
    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
    private val client = OkHttpClient()

    fun sendNotificationToUser(
        fcmToken: String,
        title: String,
        body: String,
        key: String,
        context: Context,
        type: String,
        name: String,
        civilianId: String
    ) {
        Thread {
            try {
                // 1. Load credentials from assets
                val assetManager = context.assets
                val inputStream = assetManager.open("service-account.json")
                val googleCredentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

                googleCredentials.refreshIfExpired()
                val accessToken = googleCredentials.accessToken.tokenValue

                // 2. Build JSON Payload
                val jsonBody = JSONObject().apply {
                    val message = JSONObject()

                    if (fcmToken.startsWith("/topics/")) {
                        message.put("topic", fcmToken.substring(8))
                    } else {
                        message.put("token", fcmToken)
                    }

                    val notification = JSONObject()
                    notification.put("title", title)
                    notification.put("body", body)
                    message.put("notification", notification)

                    // Data payload matches teammate's logic and your DB screenshot
                    val data = JSONObject()
                    data.put("title", title)
                    data.put("body", body)
                    data.put("complaintId", key)
                    data.put("civilianId", civilianId)
                    data.put("type", type)
                    data.put("senderName", name)

                    message.put("data", data)
                    put("message", message)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(FCM_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) Log.e("FCM_LOG", "Error: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("FCM_LOG", "Exception: ${e.message}")
            }
        }.start()
    }
}