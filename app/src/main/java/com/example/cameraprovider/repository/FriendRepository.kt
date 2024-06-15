package com.example.cameraprovider.repository

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.cameraprovider.model.Friendship
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FriendRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val pendingDynamicLinkData: FirebaseDynamicLinks = FirebaseDynamicLinks.getInstance()
    fun createFriendRequestLink(
        userId: String,
        userName: String,
        userAvt: Uri
    ): Task<ShortDynamicLink> {
        return FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://closestory.com/friendRequest?userId=$userId&userName=$userName")) // URL này phải khớp với allowed domain
            .setDomainUriPrefix("https://closestory.page.link") // Domain đã cấu hình trong Firebase Console
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder("com.example.cameraprovider").build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle("Kết bạn với mình trên Close 💌")
                    .setDescription("Close.Story❣️")
                    .setImageUrl(userAvt)  // Đặt URL của hình ảnh
                    .build()
            )
            .buildShortDynamicLink()
    }

    suspend fun createdynamicLink(): String {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid
        return if (currentUserId != null) {
            val docRef = fireStore.collection("users").document(currentUserId)
            val snapshot = docRef.get().await()
            val userName = snapshot.getString("nameUser") ?: ""
            val userAvt = snapshot.getString("avatarUser") ?: ""
            Log.d("ImageAvt", "$userAvt, $userName")
            val shortDynamicLink =
                createFriendRequestLink(currentUserId, userName, Uri.parse(userAvt)).await()
            shortDynamicLink.shortLink.toString()  // Trả về URL của shortLink

        } else {
            throw Exception("User not logged in")
        }
    }

    private fun handleDynamicLink(intent: Intent): Pair<String?, String?> {
        return try {
            val task = pendingDynamicLinkData.getDynamicLink(intent)
            val dynamicLink = task.result // Lấy kết quả từ Task
            val deepLink = dynamicLink?.link
            val userId = deepLink?.getQueryParameter("userId")
            val userName = deepLink?.getQueryParameter("userName")
            Pair(userId, userName)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    suspend fun handleFriendRequest(intent: Intent): Friendship? {
        return try {
            val (senderid, sendername) = handleDynamicLink(intent)
            if (senderid != null && sendername != null) {
                val currentUserId = auth.currentUser?.uid
                val newPostRef = fireStore.collection("friendships").document()
                val id = newPostRef.id
                val friendship = fireStore.runTransaction { transaction ->
                    val friendship = Friendship(
                        id = id,
                        uid1 = currentUserId,
                        uid2 = senderid,
                        state = "pending",
                        timeStamp = Timestamp.now()
                    )
                    transaction.set(newPostRef, friendship)

                    friendship
                }.await() // Đợi transaction hoàn thành
                friendship
            } else {
                null
            }
        } catch (e: Exception) {
            // Xử lý lỗi transaction
            null
        }
    }
}