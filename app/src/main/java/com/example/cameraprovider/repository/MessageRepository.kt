package com.example.cameraprovider.repository


import android.util.Log
import com.example.cameraprovider.Notification.MyFCMService

import com.example.cameraprovider.model.Message
import com.example.cameraprovider.model.User
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import com.google.ai.client.generativeai.Chat

//import okhttp3.*


class MessageRepository() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()


    suspend fun sendMessage(
        message: String,
        postId: String,
        receiverId: String,
        imgUrl: String,
        VoiceUrl: String,
        content: String,
        time: String,
        avtUserpost: String
    ): Result<Boolean> {
        return try {
            val currentUID =
                auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))

            val newMessageRef = fireStore.collection("messages").document()
            val messageId = newMessageRef.id
            val message = Message(
                messageId = messageId,
                senderId = currentUID,
                receiverId = receiverId,
                message = message,
                postId = postId,
                imageUrl = imgUrl,
                voiceUrl = VoiceUrl,
                timestamp = time,
                content = content,
                avtpost = avtUserpost,
                createdAt = System.currentTimeMillis().toString()
            )
            newMessageRef.set(message).await()

//            // Lấy và lưu access token của người nhận
//            val receiverUser = fireStore.collection("users").document(receiverId).get().await()
//            val receiverToken = receiverUser.toObject(User::class.java)!!.token
//
//
//            // Gửi thông báo FCM cho người nhận
//            receiverToken?.let {
//                saveUserToken(receiverId, it, message.message.toString())
//            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)

            // Handle exceptions and indicate failure
        }
    }


    fun saveUserToken(userId: String, token: String, message: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(userId)
        userRef.update("token", token)
            .addOnSuccessListener {
                Log.d("updatedtokenforuser", "$token")
                MyFCMService().sendFCMMessage(token, "Bạn có tin nhắn mới", message.toString())
            }
            .addOnFailureListener { e ->
                Log.d("updating token for user", "$userId, ${e.message}")
            }
    }

    fun getMessages(userId: String, friendId: String): Flow<List<Message>> = callbackFlow {
        val listenerRegistration = fireStore.collection("messages")
            .whereIn("receiverId", listOf(userId, friendId))
            .whereIn("senderId", listOf(userId, friendId))
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun getFriendsWithLastMessages(): Pair<List<User>, Map<String, Message>> {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Vui lòng đăng nhập!")
        val friends = mutableListOf<User>()
        val lastMessages = mutableMapOf<String, Message>()

        val friendships1 = fireStore.collection("friendships")
            .whereEqualTo("uid1", currentUserId)
            .whereEqualTo("state", "Accepted")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .get()
            .await()
        friendships1.documents.mapNotNullTo(friends) {
            val friendId = it.getString("uid2")!!
            val user = fireStore.collection("users").document(friendId).get().await()
                .toObject(User::class.java)
            if (user != null) {
                val lastMessage = getLastMessage(friendId)
                if (lastMessage != null) {
                    lastMessages[friendId] = lastMessage
                }
            }
            user
        }

        val friendships2 = fireStore.collection("friendships")
            .whereEqualTo("uid2", currentUserId)
            .whereEqualTo("state", "Accepted")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .get()
            .await()
        friendships2.documents.mapNotNullTo(friends) {
            val friendId = it.getString("uid1")!!
            val user = fireStore.collection("users").document(friendId).get().await()
                .toObject(User::class.java)
            if (user != null) {
                val lastMessage = getLastMessage(friendId)
                if (lastMessage != null) {
                    lastMessages[friendId] = lastMessage
                }
            }
            user
        }

        return Pair(friends, lastMessages)
    }

    private suspend fun getLastMessage(friendId: String): Message? {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Vui lòng đăng nhập!")

        val sentQuery = fireStore.collection("messages")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiverId", friendId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val receivedQuery = fireStore.collection("messages")
            .whereEqualTo("senderId", friendId)
            .whereEqualTo("receiverId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val lastSentMessage = if (!sentQuery.isEmpty) sentQuery.documents.first()
            .toObject(Message::class.java) else null
        val lastReceivedMessage = if (!receivedQuery.isEmpty) receivedQuery.documents.first()
            .toObject(Message::class.java) else null

        return if (lastSentMessage != null && lastReceivedMessage != null) {
            val sentTimestamp = lastSentMessage.createdAt?.toLongOrNull() ?: 0L
            val receivedTimestamp = lastReceivedMessage.createdAt?.toLongOrNull() ?: 0L

            if (sentTimestamp > receivedTimestamp) {
                lastSentMessage
            } else {
                lastReceivedMessage
            }
        } else {
            lastSentMessage ?: lastReceivedMessage
        }
    }


    //gemini
    private var chat: Chat? = null

    private val chatGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyAQcbXn_9eZiTQASKCUmKeNHN1_kvmo6eQ"
    )
    suspend fun generateResponse(prompt: String): String {
        val response = withContext(Dispatchers.IO) {
            if (chat == null) {
                chat = chatGenerativeModel.startChat()
            }
            chat!!.sendMessage(prompt)
        }
        return response.text.toString()
    }

    suspend fun sendMessageToGemini(prompt: String): Result<Message> {
        return try {
            val userMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = auth.currentUser?.uid,
                receiverId = "Gemini",
                message = prompt,
                createdAt = System.currentTimeMillis().toString()
            )
            val userMessageRef = fireStore.collection("messages").document(userMessage.messageId!!)
            userMessageRef.set(userMessage).await()

            val responseText = generateResponse(prompt)


            val geminiMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = "Gemini",
                receiverId = auth.currentUser?.uid,
                message = responseText,
                createdAt = System.currentTimeMillis().toString()
            )

            val geminiMessageRef =
                fireStore.collection("messages").document(geminiMessage.messageId!!)
            geminiMessageRef.set(geminiMessage).await()

            Result.success(geminiMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}