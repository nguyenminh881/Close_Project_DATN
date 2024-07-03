package com.example.cameraprovider.repository


import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity

import com.example.cameraprovider.model.Message
import com.example.cameraprovider.model.MessageStatus
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.coroutineScope
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

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
                createdAt = System.currentTimeMillis().toString(),
                status = MessageStatus.SENDING
            )
            newMessageRef.set(message).await()


            message.status = MessageStatus.SENT
            newMessageRef.update("status", message.status.name).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    suspend fun updateMessagesToSeen(senderId: String) {
        try {
            val messagesRef = fireStore.collection("messages")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", auth.currentUser?.uid!!)
                .whereNotEqualTo("status", MessageStatus.READ.name)

            val querySnapshot = messagesRef.get().await()
            for (document in querySnapshot.documents) {
                document.reference.update("status", MessageStatus.READ.name).await()
                Log.d("NotificationRepositoryupdate", "success updating message status")
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error updating message status", e)
        }
    }
    suspend fun updateMessagesToRead(senderId: String) {
        try {

            val currentUID = auth.currentUser?.uid?: return
            val messagesRef = fireStore.collection("messages")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("receiverId", currentUID)
                .whereNotEqualTo("status", MessageStatus.READ.name)

            val querySnapshot = messagesRef.get().await()
            for (document in querySnapshot.documents) {
                document.reference.update("status", MessageStatus.READ.name).await()
            }


            val messagesRefReverse = fireStore.collection("messages")
                .whereEqualTo("senderId", currentUID)
                .whereEqualTo("receiverId", senderId)
                .whereNotEqualTo("status", MessageStatus.READ.name)

            val querySnapshotReverse = messagesRefReverse.get().await()
            for (document in querySnapshotReverse.documents) {
                document.reference.update("status", MessageStatus.READ.name).await()
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error updating message status", e)
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




    fun getFriendsWithLastMessages(
        onSuccess: (Pair<List<User>, Map<String, Message>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val friends = mutableListOf<User>()
        val lastMessages = mutableMapOf<String, Message>()
        val friendsAndMessages = Pair(friends, lastMessages)

        fun checkIfComplete(totalFriends: Int) {
            if (friends.size == totalFriends && lastMessages.size == totalFriends) {
                // sap ds theo tn cuoi
                val sortedFriends = friends.sortedByDescending { user ->
                    lastMessages[user.UserId]?.createdAt?.toLongOrNull() ?: 0L
                }
                Log.d("getFriendsWithLastMessages", "Friends: $sortedFriends")
                Log.d("getFriendsWithLastMessages", "Last Messages: $lastMessages")
                onSuccess(Pair(sortedFriends, lastMessages))
            }else {
                val sortedFriends = friends.sortedBy { user ->
                    lastMessages[user.UserId]?.createdAt?.toLongOrNull() ?: 0L
                }
                Log.d("getFriendsWithLastMessages", "Friends with no last messages: $sortedFriends")
                Log.d("getFriendsWithLastMessages", "Last Messages: $lastMessages")
                onSuccess(Pair(sortedFriends, lastMessages))
            }
        }

        val friendIds = mutableSetOf<String>()
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Vui lòng đăng nhập!")

        fun fetchFriendDetails(friendId: String) {
            fireStore.collection("users").document(friendId).get()
                .addOnSuccessListener { userDoc ->
                    val user = userDoc.toObject(User::class.java)
                    user?.let { friends.add(it) }
                    checkIfComplete(friendIds.size)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        }

        fun fetchLastMessage(friendId: String) {
            val sentMessagesQuery = fireStore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", friendId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)

            val receivedMessagesQuery = fireStore.collection("messages")
                .whereEqualTo("senderId", friendId)
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)

            val allMessagesQuery = listOf(sentMessagesQuery, receivedMessagesQuery)

            allMessagesQuery.forEach { query ->
                query.addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        onError(exception)
                        return@addSnapshotListener
                    }

                    val lastMessage = snapshot?.documents?.firstOrNull()?.toObject(Message::class.java)
                    Log.d("fetchLastMessage", " LastMessage: $lastMessage")
                    lastMessage?.let {
                        val existingMessage = lastMessages[friendId]
                        if (existingMessage == null || it.createdAt?.toLongOrNull() ?: 0L > existingMessage.createdAt?.toLongOrNull() ?: 0L) {
                            lastMessages[friendId] = it
                            Log.d("fetchLastMessage", "Updated lastMessages: ${lastMessages}")
                            checkIfComplete(friendIds.size)  // Kiểm tra điều kiện hoàn thành
                        }
                    }?: run {
                        checkIfComplete(friendIds.size)
                    }
                }
            }
        }

        fun processFriendshipDocuments(snapshot: QuerySnapshot) {
            snapshot.documents.forEach { document ->
                val friendId = if (document.getString("uid1") == currentUserId) {
                    document.getString("uid2")
                } else {
                    document.getString("uid1")
                }
                friendId?.let { id ->
                    if (friendIds.add(id)) {
                        fetchFriendDetails(id)
                        fetchLastMessage(id)
                    }
                }
            }
            checkIfComplete(friendIds.size)
        }

        val queries = listOf(
            fireStore.collection("friendships")
                .whereEqualTo("uid1", currentUserId)
                .whereEqualTo("state", "Accepted"),
            fireStore.collection("friendships")
                .whereEqualTo("uid2", currentUserId)
                .whereEqualTo("state", "Accepted")
        )

        queries.forEach { query ->
            query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onError(exception)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    processFriendshipDocuments(it)
                }
            }
        }
    }



    //gemini
    private var chat: Chat? = null

    private val chatGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyAQcbXn_9eZiTQASKCUmKeNHN1_kvmo6eQ"
    )
    val currentTime = LocalTime.now()
    val currentDate = LocalDate.now()
    val dayOfWeek = when (currentDate.dayOfWeek) {
        DayOfWeek.MONDAY -> "thứ Hai"
        DayOfWeek.TUESDAY -> "thứ Ba"
        DayOfWeek.WEDNESDAY -> "thứ Tư"
        DayOfWeek.THURSDAY -> "thứ Năm"
        DayOfWeek.FRIDAY -> "thứ Sáu"
        DayOfWeek.SATURDAY -> "thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ nhật"
    }
    val dayOfMonth = currentDate.dayOfMonth

    val month = when(currentDate.monthValue){
        1 -> "Tháng Một"
        2 -> "Tháng Hai"
        3-> "Tháng Ba"
        4 -> "Tháng Tư"
        5 -> "Tháng Năm"
        6 -> "Tháng Sáu"
        7 -> "Tháng Bảy"
        8 -> "Tháng Tám"
        9 -> "Tháng Chín"
        10 -> "Tháng Mười"
        11 -> "Tháng Mười Một"
        else -> "Tháng Mười Hai"
    }
    val year = currentDate.year
    val timeOfDay = when {
        currentTime.isAfter(LocalTime.of(4, 0)) && currentTime.isBefore(LocalTime.of(11, 0)) -> "buổi sáng thứ $dayOfWeek, ngày $dayOfMonth tháng $month năm $year"
        currentTime.isAfter(LocalTime.of(11, 0)) && currentTime.isBefore(LocalTime.of(13, 30)) -> "buổi trưa thứ $dayOfWeek, ngày $dayOfMonth tháng $month năm $year"
        currentTime.isAfter(LocalTime.of(13, 30)) && currentTime.isBefore(LocalTime.of(18, 30)) -> "buổi chiều thứ $dayOfWeek, ngày $dayOfMonth tháng $month năm $year"
        currentTime.isAfter(LocalTime.of(18, 30)) && currentTime.isBefore(LocalTime.of(23, 59)) -> "buổi tối thứ $dayOfWeek, ngày $dayOfMonth tháng $month năm $year"
        else -> "đêm khuya thứ $dayOfWeek, ngày $dayOfMonth tháng $month năm $year"
    }


    suspend fun generateResponse(prompt: String): String {
        val daytime = listOf("giờ", "buổi", "sáng", "trưa", "chiều", "tối", "đêm", "hôm nay", "ngày mai", "hôm qua", "thứ","ngày","tháng","năm")
        val timeRegex = daytime.joinToString("|") { "\\b$it\\b" }.toRegex()

        val modifiedPrompt = if (timeRegex.containsMatchIn(prompt)) {
            "Yêu cầu nhận định đúng thời gian là $timeOfDay khi nhận được câu hỏi về thời gian. $prompt"
        } else {
            prompt
        }

        val response = withContext(Dispatchers.IO) {
            if (chat == null) {
                chat = chatGenerativeModel.startChat()
            }
            chat!!.sendMessage("tên của bạn là Close chat bot nếu Prompt có hỏi bạn là ai hoặc tên gì" +
                    "Trò chuyện vui vẻ, tự nhiên nhất có thể" +
                    "Thêm các biểu cảm icon cho xinh động" +
                    "Tâm sự, đồng cảm với người trò truyện" +
                    "Ưu tiên trả lời bằng tiếng Việt, trả lời thân thiện" +
                    "Ưu tiên độ chính xác nếu câu hỏi câu hỏi thông tin" +
                    "Ưu tiên sự sáng tạo nếu là câu hỏi cần sáng tạo" +
                    "Hãy coi như là mình biết thông tin thời gian này." +
                    "Không được nhắc đến thông tin thời gian liên tục chỉ dùng khi thật sự cần thôi" +
                    "Bạn cần thêm sự sáng tạo vui vẻ thay vì nhắc đến thời gian liên tục như thế" +
                    modifiedPrompt+prompt)
        }
        return response.text.toString()
    }


    suspend fun sendMessageToGemini(prompt: String,
                                    onComplete: (Result<Message>) -> Unit

    ){
         try {

            val userMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = auth.currentUser?.uid,
                receiverId = "Gemini",
                message = prompt,
                createdAt = System.currentTimeMillis().toString(),
                status = MessageStatus.SENDING
            )
            val userMessageRef = fireStore.collection("messages").document(userMessage.messageId!!)
            userMessageRef.set(userMessage).await()
            userMessage.status = MessageStatus.SENT
            userMessageRef.update("status", userMessage.status.name).await()


            val responseText = generateResponse(prompt)
            val geminiMessage = Message(
                messageId = UUID.randomUUID().toString(),
                senderId = "Gemini",
                receiverId = auth.currentUser?.uid,
                message = responseText,
                createdAt = System.currentTimeMillis().toString(),
                status = MessageStatus.SENDING
            )

            val geminiMessageRef =
                fireStore.collection("messages").document(geminiMessage.messageId!!)
            geminiMessageRef.set(geminiMessage).await()
            geminiMessage.status = MessageStatus.SENT
            geminiMessageRef.update("status", geminiMessage.status.name).await()
             onComplete(Result.success(geminiMessage))
        } catch (e: Exception) {
             onComplete(Result.failure(e))
        }
    }


}