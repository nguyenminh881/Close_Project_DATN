package com.example.cameraprovider.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.cameraprovider.model.Post
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.cameraprovider.model.Like
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.Query

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class PostRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    //add pót
    suspend fun addPost(contentUri: Uri, content: String, isImage: Boolean): PostResult {
        return try {
            val uniqueID = UUID.randomUUID().toString()
            val fileName = "post_${auth.currentUser!!.uid}$uniqueID"

            val storageReference = if (isImage) {
                storage.reference.child("${auth.currentUser!!.uid}/post_image/$fileName.jpeg")
            } else {
                storage.reference.child("${auth.currentUser!!.uid}/post_voice/$fileName.aac")
            }
            val uploadTask = storageReference.putFile(contentUri).await()
            val downloadUrl = storageReference.downloadUrl.await()
            Log.d("TAGY", "Download URL obtained: $downloadUrl")
            val currentUID = auth.currentUser?.uid
            val timeStamp = Timestamp(Date())
            if (currentUID != null) {
                val docRef = fireStore.collection("users").document(currentUID).get().await()
                val currenName = docRef.getString("nameUser")
                val currentAvt = docRef.getString("avatarUser")
                val newPostRef = fireStore.collection("posts").document()
                val postId = newPostRef.id
                val post = Post(
                    postId = postId,
                    userId = currentUID,
                    userName = currenName,
                    userAvatar = currentAvt,
                    content = content,
                    imageURL = if (isImage) downloadUrl.toString() else "",
                    voiceURL = if (!isImage) downloadUrl.toString() else "",
                    createdAt = timeStamp,
                    hiddenForUsers = emptyList()
                )
                newPostRef.set(post).await()
                PostResult.Success(postId = "")
            } else {
                PostResult.Failure("User ID is null")
            }
        } catch (e: FirebaseNetworkException) {
            PostResult.Failure(e.message ?: "Kiểm tra kết nối mạng")
        }
    }

    fun iscurrentId(): String {
        return auth.currentUser!!.uid
    }

    //cau hinh paging3
    fun getPosts(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 10,
                initialLoadSize = 10,
                maxSize = 100,
                jumpThreshold = 10
            ),
            pagingSourceFactory = { PostPagingSource(fireStore, auth) }
        ).flow
    }


    private val likeMutex = Mutex()

    suspend fun updateLikePost(postId: String, icon: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return

        val fireStore = FirebaseFirestore.getInstance()
        val timeStamp = Timestamp(Date())

        likeMutex.withLock {
            try {
                fireStore.runTransaction { transaction ->
                    // Xem bản ghi "like" hiện có
                    val likeQuerySnapshot = Tasks.await(
                        fireStore.collection("likes")
                            .whereEqualTo("postId", postId)
                            .whereEqualTo("userId", userId)
                            .get()
                    )

                    if (likeQuerySnapshot.isEmpty) {
                        // Tạo bản ghi "like" mới nếu chưa tồn tại
                        val postDocument = transaction.get(fireStore.collection("posts").document(postId))
                        val ownerId = postDocument.getString("userId") ?: ""
                        val newLike = Like(
                            postId = postId,
                            userId = userId,
                            ownerId = ownerId,
                            reactions = listOf(icon),
                            createdAt = timeStamp
                        )
                        transaction.set(fireStore.collection("likes").document(), newLike)
                    } else {
                        // Cập nhật bản ghi "like" hiện có
                        val likeDocumentSnapshot = likeQuerySnapshot.documents[0]
                        val existingLike = likeDocumentSnapshot.toObject(Like::class.java)

                        if (existingLike != null) {
                            val updatedReactions = if (existingLike.reactions.size >= 4) {
                                val updatedList = existingLike.reactions.toMutableList()
                                updateReactions(updatedList, icon)
                                updatedList
                            } else {
                                existingLike.reactions.toMutableList().apply { add(icon) }
                            }

                            transaction.update(likeDocumentSnapshot.reference, mapOf(
                                "reactions" to updatedReactions,
                                "createdAt" to timeStamp
                            ))
                        } else {
                            throw IllegalStateException("Lỗi")
                        }
                    }
                }.await() // Đây là nơi bạn chờ đợi giao dịch hoàn thành
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun updateReactions(reactions: MutableList<String>, newIcon: String) {
        if (reactions.size >= 4) {
            reactions.removeAt(0)
        }
        reactions.add(newIcon)
    }


    fun getlikepost(postId: String, listinfolike: (List<Pair<String, List<String>>>) -> Unit) {
        val likesRef = fireStore.collection("likes").whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        likesRef.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }
            val reactionList = mutableListOf<Pair<String, List<String>>>()

            querySnapshot?.documents?.forEach { likeDoc ->
                val like = likeDoc.toObject(Like::class.java)
                like?.let {
                    val userId = like.userId
                    //laya username
                    fireStore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            if (userSnapshot.exists()) {
                                val userName = userSnapshot.getString("nameUser") ?: ""
                                val reactions = like.reactions

                                // Tạo pair userName và reactions
                                val userReactions = Pair(userName, reactions)
                                reactionList.add(userReactions)

                                if (reactionList.size == querySnapshot.size()) {
                                    listinfolike(reactionList)
                                }
                            }
                        }.addOnFailureListener { exception ->
                            Log.e("PostRepository", "Lỗi khi lấy like: ${exception.message}", exception)
                        }
                }
            }
        }
    }

    private val imageGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyAQcbXn_9eZiTQASKCUmKeNHN1_kvmo6eQ",
        generationConfig = generationConfig {
            temperature = 0.8f
            maxOutputTokens = 80
        }
    )
    suspend fun generateContentFromImage(imageBytes: Bitmap): String {
        val inputContent = content {
            image(imageBytes)
            text("Viết một caption ngắn (dưới 100 ký tự) và sáng tạo cho bức ảnh này, tập trung vào [mô tả ngắn gọn nội dung chính của bức ảnh]. " +
                    "Nếu bức ảnh thể hiện tâm trạng con người, hãy diễn đạt tâm trạng bằng cách sử dụng ngôn ngữ hình ảnh, ẩn dụ, hoặc so sánh. " +
                    "Nếu là phong cảnh, hoạt động, hay món ăn, hãy mô tả và có thể thêm 1-2 biểu tượng cảm xúc phù hợp. " +
                    "Phải phù hợp với việc chia sẻ đến mọi người" +
                    " Không tạo ra hastag" +
                    "Vì người chụp bức ảnh đó muốn chia sẻ đến mới mọi người nên ưu tiên ngôi kể thứ nhất" +
                    "Nếu bức ảnh đó chứa một chân dung duy nhất thì có khả năng người chụp ảnh sẽ muốn mô tả tâm trạng bản thân, ưu tiên dùng ngôi thứ 1" +
                    "Bắt buộc phải sử dụng Tiếng Việt")
        }
        val response = imageGenerativeModel.generateContent(inputContent)
        return response.text.toString()
    }

    sealed class PostResult {
        data class Success(val postId: String) : PostResult()
        data class Failure(val error: String) : PostResult()
    }


    fun observePost(postId: String) {
        val postRef = fireStore.collection("posts").document(postId)
        postRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val post = snapshot.toObject(Post::class.java)


            } else {
                Log.d("postrepo", "Current data: null")
            }
        }
    }


    suspend fun getLatestPost(): Post? {
        val snapshot = fireStore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return if (!snapshot.isEmpty) {
            snapshot.documents[0].toObject(Post::class.java)
        } else {
            null
        }
    }


    suspend fun deletePost(postId: String): Boolean {
        val postRef = fireStore.collection("posts").document(postId)
        val currentId = auth.currentUser?.uid

        return if (currentId != null) {
            try {
                val postSnapshot = postRef.get().await()
                if (postSnapshot.exists()) {
                    val post = postSnapshot.toObject(Post::class.java)
                    if (post != null) {
                        if (post.userId == currentId) {
                            when {
                                post.imageURL == null && post.voiceURL != null -> {
                                    val voiceRef = storage.getReferenceFromUrl(post.voiceURL)
                                    voiceRef.delete().await()
                                }
                                post.imageURL != null && post.voiceURL == null -> {
                                    val imageRef = storage.getReferenceFromUrl(post.imageURL)
                                    imageRef.delete().await()
                                }
                            }

                            //xoa like
                            val likesSnapshot = fireStore.collection("likes")
                                .whereEqualTo("postId", postId)
                                .get()
                                .await()
                            if (!likesSnapshot.isEmpty) {
                                likesSnapshot.documents.forEach { it.reference.delete().await() }
                            }

                            // xoa noi dung cmt
                            val messagesSnapshot = fireStore.collection("messages")
                                .whereEqualTo("postId", postId)
                                .get()
                                .await()
                            if (!messagesSnapshot.isEmpty) {
                                messagesSnapshot.documents.forEach {
                                    it.reference.update(
                                        mapOf(
                                            "postId" to "",
                                            "imageUrl" to "",
                                            "voiceUrl" to "",
                                            "timestamp" to "",
                                            "content" to "",
                                            "avtpost" to ""
                                        )
                                    ).await()
                                }
                            }


                            postRef.delete().await()
                            true
                        } else {
                            postRef.update("hiddenForUsers", FieldValue.arrayUnion(currentId)).await()
                            true
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.d("PostRepository", "Error deleting post: ${e}")
                false
            }
        } else {
            false
        }
    }


}