package com.example.cameraprovider.repository

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.example.cameraprovider.model.Friendship
import com.example.cameraprovider.model.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.dynamicLinks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.net.URLEncoder

class FriendRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val pendingDynamicLinkData: FirebaseDynamicLinks = FirebaseDynamicLinks.getInstance()
    fun createFriendRequestLink(
        userId: String, userName: String,
        userAvt: Uri
    ): Task<ShortDynamicLink> {
        val deepLink =
            "https://close-project-15ed2.web.app/friendRequest?userId=$userId"

        // Build dynamic link
        val dynamicLink = pendingDynamicLinkData.createDynamicLink()
            .setDomainUriPrefix("https://closeme.page.link")
            .setLink(Uri.parse(deepLink))
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder("com.example.cameraprovider")
                    .build()
            )
            .setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle("Kết bạn với mình trên Close 💌")
                    .setDescription("Close.Story❣️")
                    .setImageUrl(userAvt)
                    .build()
            )
            .setNavigationInfoParameters(
                DynamicLink.NavigationInfoParameters.Builder()
                    .setForcedRedirectEnabled(true)
                    .build()
            )

        // Build short dynamic link
        return dynamicLink.buildShortDynamicLink()
    }

    suspend fun createdynamicLink(): String {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid
        return if (currentUserId != null) {
            val docRef = fireStore.collection("users").document(currentUserId)
            val snapshot = docRef.get().await()
            val userName = snapshot.getString("nameUser") ?: ""
            val userAvt = snapshot.getString("avatarUser") ?: ""
            Log.d("ImageAvt", "id = $currentUserId, img = $userAvt")

            val shortDynamicLink =
                createFriendRequestLink(currentUserId, userName, Uri.parse(userAvt)).await()
            shortDynamicLink.shortLink.toString()
        } else {
            throw Exception("User not logged in")
        }
    }

    fun handleDynamicLink(intent: Intent, callback: (String?) -> Unit) {
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->
                val deepLink = pendingDynamicLinkData?.link
                if (deepLink != null) {
                    val userId = deepLink?.getQueryParameter("userId")
                    Log.d("DeepLink", "userId: $userId")
                    callback(userId)
                } else {
                    // Thử lấy App Link sau một khoảng thời gian ngắn
                    val appLinkAction: String? = intent.action
                    val appLinkData: Uri? = intent.data
                    if (appLinkAction == Intent.ACTION_VIEW && appLinkData != null) {
                        val userId = appLinkData.getQueryParameter("userId")
                        Log.d("AppLink", "userId: $userId")
                        callback(userId)
                    } else {
                        callback(null)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("LinkHandling", "getDynamicLink:onFailure", e)
                callback(null)
            }
    }

    suspend fun getAvatarUsersendlink(userId: String?): Pair<String?, String?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.uid != userId) {
                val getdoc =
                    userId?.let { fireStore.collection("users").document(it).get().await() }
                val avtUser = getdoc?.getString("avatarUser")
                val nameUser = getdoc!!.getString("nameUser")
                return Pair(nameUser, avtUser)
            } else {
                return Pair(null, null)
            }

        } catch (e: FirebaseException) {
            return Pair(null, null)
        }
    }

    suspend fun handleFriendRequest(senderId: String?): Result<Friendship> {
        return try {
            val currentUserId = auth.currentUser?.uid

            if (senderId != null && currentUserId != null) {

                val friendCount1 = fireStore.collection("friendships")
                    .whereIn("uid1", listOf(currentUserId, senderId))
                    .whereIn("uid2", listOf(currentUserId, senderId))
                    .whereEqualTo("state", "Accepted")
                    .get()
                    .await()
                    .documents
                    .size

                if (friendCount1 >= 15) {
                    return Result.failure(Exception("Bạn hoặc người bạn muốn kết bạn đã đạt giới hạn 15 bạn bè"))
                }


                // Kiểm tra xem đã tồn tại yêu cầu kết bạn hay chưa
                val existingFriendship = fireStore.collection("friendships")
                    .whereEqualTo("uid1", currentUserId)
                    .whereEqualTo("uid2", senderId)
                    .whereEqualTo("state", "Accepted")
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                if (existingFriendship != null) {
                    // Đã tồn tại yêu cầu kết bạn, trả về lỗi hoặc thông báo
                    Result.failure(Exception("Đã là bạn bè rồi nhé"))
                } else {
                    val getdoc = fireStore.collection("users").document(currentUserId).get().await()
                    // Chưa có yêu cầu kết bạn, tạo mới
                    val newPostRef = fireStore.collection("friendships").document()
                    val id = newPostRef.id
                    val friendship = fireStore.runTransaction { transaction ->
                        val friendship = Friendship(
                            id = id,
                            uid1 = currentUserId,
                            uid2 = senderId,
                            state = "pending",
                            userAvt = getdoc?.getString("avatarUser"),
                            userName = getdoc!!.getString("nameUser"),
                            timeStamp = Timestamp.now()
                        )
                        transaction.set(newPostRef, friendship)
                        friendship
                    }.await()
                    Result.success(friendship)
                }
            } else {
                Result.failure(Exception("Vui lòng đăng nhập!"))
            }
        } catch (e: Exception) {
            // Xử lý lỗi transaction
            Result.failure(e)
        }
    }


//    suspend fun handleFriendRequest(senderId: String?): Result<Friendship> {
//        return try {
//            val currentUserId = auth.currentUser?.uid
//
//            if (senderId != null && currentUserId != null) {
//                // Kiểm tra xem đã tồn tại yêu cầu kết bạn hay chưa
//                val existingFriendship = fireStore.collection("friendships")
//                    .whereEqualTo("uid1", currentUserId)
//                    .whereEqualTo("uid2", senderId)
//                    .whereEqualTo("state", "Accepted")
//                    .get()
//                    .await()
//                    .documents
//                    .firstOrNull()
//                if (existingFriendship != null) {
//                    // Đã tồn tại yêu cầu kết bạn, trả về lỗi hoặc thông báo
//                    Result.failure(Exception("Đã là bạn bè rồi nhé"))
//                } else {
//                    val getdoc = fireStore.collection("users").document(currentUserId).get().await()
//                    // Chưa có yêu cầu kết bạn, tạo mới
//                    val newPostRef = fireStore.collection("friendships").document()
//                    val id = newPostRef.id
//                    val friendship = fireStore.runTransaction { transaction ->
//                        val friendship = Friendship(
//                            id = id,
//                            uid1 = currentUserId,
//                            uid2 = senderId,
//                            state = "pending",
//                            userAvt = getdoc?.getString("avatarUser"),
//                            userName = getdoc!!.getString("nameUser"),
//                            timeStamp = Timestamp.now()
//                        )
//                        transaction.set(newPostRef, friendship)
//                        friendship
//                    }.await()
//                    Result.success(friendship)
//                }
//            } else {
//                Result.failure(Exception("Vui lòng đăng nhập!"))
//            }
//        } catch (e: Exception) {
//            // Xử lý lỗi transaction
//            Result.failure(e)
//        }
//    }


    fun getFriendships(callback: (Result<MutableList<Friendship>?>) -> Unit) {
        val currentUserId = auth.currentUser?.uid
            ?: return callback(Result.failure(Exception(" Vui lòng đăng nhập!")))
        fireStore.collection("friendships")
            .whereEqualTo("uid2", currentUserId)
            .whereEqualTo("state", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    callback(Result.failure(e))
                    return@addSnapshotListener
                }
                val friendships = mutableListOf<Friendship>()
                snapshot?.documents?.forEach {
                    val friendship = it.toObject(Friendship::class.java)
                    friendship?.let { friendships.add(it) }
                }
                callback(Result.success(friendships))
            }
    }

    suspend fun updatestateFriendship(state: String, friendshipId: String): Result<Friendship?> {
        return try {
            val currentUserId =
                auth.currentUser?.uid ?: return Result.failure(Exception("Vui lòng đăng nhập!"))

            val result = fireStore.runTransaction { transaction ->
                val friendshipRef = fireStore.collection("friendships").document(friendshipId)
                val friendshipSnapshot = transaction.get(friendshipRef)

                if (friendshipSnapshot.exists()) {
                    val currentFriendship = friendshipSnapshot.toObject(Friendship::class.java)
                    if (currentFriendship?.state == "pending") {
                        if (state == "Declined") {
                            // Xóa
                            transaction.delete(friendshipRef)
                            null
                        } else if (state == "Accepted") {
                            val updates = mapOf("state" to state)
                            transaction.update(friendshipRef, updates)
                            currentFriendship
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun getFriendAccepted(onSuccess: (MutableList<User>) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val currentUserId = auth.currentUser?.uid
                ?: throw Exception("Vui lòng đăng nhập!")

            Log.d("FriendRepository", "Đang lấy danh sách bạn bè cho người dùng $currentUserId")

            val query1 = fireStore.collection("friendships")
                .whereEqualTo("uid1", currentUserId)
                .whereEqualTo("state", "Accepted")
                .orderBy("timeStamp", Query.Direction.DESCENDING)

            val query2 = fireStore.collection("friendships")
                .whereEqualTo("uid2", currentUserId)
                .whereEqualTo("state", "Accepted")
                .orderBy("timeStamp", Query.Direction.DESCENDING)

            val combinedTask = Tasks.whenAllSuccess<QuerySnapshot>(query1.get(), query2.get())
            combinedTask.addOnSuccessListener { results ->
                val friendIds = mutableListOf<String>()
                for (result in results) {
                    result.documents.forEach { doc ->
                        val uid1 = doc.getString("uid1")
                        val uid2 = doc.getString("uid2")
                        if (uid1 != currentUserId && uid1 != null) {
                            friendIds.add(uid1)
                        } else if (uid2 != currentUserId && uid2 != null) {
                            friendIds.add(uid2)
                        }
                    }
                }

                Log.d("FriendRepository", "Tìm thấy ${friendIds.size} bạn bè")

                if (friendIds.isNotEmpty()) {
                    val friendRefs = friendIds.map { friendId ->
                        fireStore.collection("users").document(friendId)
                    }
                    val friendSnapshots = mutableListOf<User>()
                    var count = 0
                    friendRefs.forEach { ref ->
                        ref.get().addOnSuccessListener { friendSnapshot ->
                            friendSnapshot.toObject(User::class.java)?.let { friend ->
                                friendSnapshots.add(friend)
                            }
                            count++
                            if (count == friendRefs.size) {
                                onSuccess(friendSnapshots)
                            }
                        }.addOnFailureListener { exception ->
                            onFailure(exception)
                        }
                    }
                } else {
                    Log.d("FriendRepository", "Không có bạn bè nào")
                    onSuccess(mutableListOf())
                }
            }.addOnFailureListener { exception ->
                Log.e("FriendRepository", "Lỗi khi lấy danh sách bạn bè", exception)
                onFailure(exception)
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Lỗi khi lấy danh sách bạn bè", e)
            onFailure(e)
        }
    }



    suspend fun removeFriend(friendId: String, position: Int): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(Exception("Vui lòng đăng nhập!"))

            val friendsCollection = fireStore.collection("friendships")
            val query1 =
                friendsCollection.whereEqualTo("uid1", currentUserId).whereEqualTo("uid2", friendId)
            val query2 =
                friendsCollection.whereEqualTo("uid1", friendId).whereEqualTo("uid2", currentUserId)

            val batch = fireStore.batch()
            val query1Snapshot = query1.get().await()
            for (docu in query1Snapshot.documents) {
                batch.delete(docu.reference)
            }

            val query2Snapshot = query2.get().await()
            for (docu in query2Snapshot.documents) {
                batch.delete(docu.reference)
            }

            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Lỗi khi xóa bạn bè", e)
            Result.failure(e)
        }
    }


    suspend fun getFriendId(position: Int): Triple<String?, String?, String?>? {
        return try {
            val currentUser = auth.currentUser
            val currentUserId = currentUser?.uid ?: return null

            val friendIds = mutableListOf<String>()
            val friendships1 = fireStore.collection("friendships")
                .whereEqualTo("uid1", currentUserId)
                .whereEqualTo("state", "Accepted")
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .get()
                .await()
            friendships1.documents.mapNotNullTo(friendIds) { it.getString("uid2") }

            val friendships2 = fireStore.collection("friendships")
                .whereEqualTo("uid2", currentUserId)
                .whereEqualTo("state", "Accepted")
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .get()
                .await()
            friendships2.documents.mapNotNullTo(friendIds) { it.getString("uid1") }

            if (position in friendIds.indices) {
                val friendId = friendIds[position]
                val friendSnapshot = fireStore.collection("users").document(friendId).get().await()
                val friend = friendSnapshot.toObject(User::class.java)
                Triple(friendId, friend?.nameUser, friend?.avatarUser)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("FriendRepository", "$e")
            null
        }
    }

}