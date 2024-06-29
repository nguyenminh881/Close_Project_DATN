package com.example.cameraprovider.Notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MyFCMService {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val fcmUrl = "https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"

    fun sendFCMMessage(deviceToken: String, title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = buildNotificationPayload(deviceToken, title, body)
                val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .header("Authorization", "Bearer ${getAccessToken()}")
                    .url(fcmUrl.replace("{project_id}", "close-project-15ed2"))
                    .post(requestBody)
                    .build()

                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    Log.d("FCM", "Notification sent successfully")
                } else {
                    Log.e("FCM", "Error sending notification: ${response.code}")
                    // In ra mã lỗi để debug
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending notification: ${e.message}", e)
            }
        }
    }
    private suspend fun getAccessToken(): String {
        return withContext(Dispatchers.IO) {
            val firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser
                ?: throw Exception("User not authenticated")


            val tokenResult = currentUser.getIdToken(false).await()
            Log.d("FCM", "Getting access token ${tokenResult}")
            tokenResult.token ?: throw Exception("Failed to get access token")
        }
    }

    private fun buildNotificationPayload(deviceToken: String, title: String, body: String): String {
        val data = mapOf(
            "to" to deviceToken,
            "notification" to mapOf(
                "title" to title,
                "body" to body
            )
        )
        return gson.toJson(data)
    }
}